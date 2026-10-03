package com.carlos.financasdescomplicadas.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa a API de ponta a ponta (H2 em memória, perfil "test"), usando o usuário
 * criado pelo UsuarioInicialConfig a partir de application-test.properties.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FluxoPrincipalIntegrationTest {

    // O MockMvc lê respostas JSON sem charset como ISO-8859-1; força UTF-8 por causa dos acentos
    @TestConfiguration
    static class Utf8Config {
        @Bean
        MockMvcBuilderCustomizer respostasEmUtf8() {
            return builder -> builder.defaultResponseCharacterEncoding(StandardCharsets.UTF_8);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    private String token;

    @BeforeEach
    void login() throws Exception {
        String resposta = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "teste@financas.dev", "senha": "senha-de-teste"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.nome").value("Conta Teste"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        token = "Bearer " + JsonPath.read(resposta, "$.token");
    }

    @Test
    void loginComSenhaErradaRetorna401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "teste@financas.dev", "senha": "errada"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Email ou senha inválidos"));
    }

    @Test
    void rotasProtegidasExigemToken() throws Exception {
        mockMvc.perform(get("/transacoes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/transacoes").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioNovoJaTemCategoriasPadrao() throws Exception {
        mockMvc.perform(get("/categorias").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nome == 'Alimentação')].tipo").value("DESPESA"));
    }

    @Test
    void criaTransacaoEValidaTipoDaCategoria() throws Exception {
        String categoriaReceitaId = criarCategoria("Freelas", "RECEITA");

        // Despesa (valor negativo) em categoria de receita é recusada
        mockMvc.perform(post("/transacoes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao": "Mercado", "valor": -80.00, "data": "2026-09-10", "categoriaId": "%s"}
                                """.formatted(categoriaReceitaId)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/transacoes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao": "Projeto site", "valor": 1500.00, "data": "2026-09-10", "categoriaId": "%s"}
                                """.formatted(categoriaReceitaId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("RECEITA"))
                .andExpect(jsonPath("$.categoriaNome").value("Freelas"))
                .andExpect(jsonPath("$.pendente").value(false));
    }

    @Test
    void validacaoDoCorpoRetornaCamposComErro() throws Exception {
        mockMvc.perform(post("/transacoes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\": \"\", \"valor\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.descricao").exists())
                .andExpect(jsonPath("$.campos.valor").exists())
                .andExpect(jsonPath("$.campos.data").exists());
    }

    @Test
    void importacaoAprendeComACorrecaoManual() throws Exception {
        String categoriaId = criarCategoria("Delivery", "DESPESA");
        String csv = """
                date,title,amount
                2026-08-03,Zé Delivery Pedido,32.00
                2026-08-17,ZÉ DELIVERY Pedido 7,19.90
                """;

        // 1ª importação: sem regra, as transações ficam pendentes
        String resposta = mockMvc.perform(multipart("/importacoes")
                        .file(arquivo("fatura-agosto.csv", csv))
                        .header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importacao.formato").value("CSV"))
                .andExpect(jsonPath("$.importacao.importadoEm").isNotEmpty())
                .andExpect(jsonPath("$.pendentes").value(2))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String importacaoId = JsonPath.read(resposta, "$.importacao.id");

        String pendentes = mockMvc.perform(get("/transacoes").param("pendentes", "true")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<String> ids = JsonPath.read(pendentes, "$[?(@.descricao == 'Zé Delivery Pedido')].id");
        String transacaoId = ids.get(0);

        // Correção manual com palavra-chave curta vira regra
        mockMvc.perform(patch("/transacoes/" + transacaoId + "/categoria").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoriaId": "%s", "palavraChave": "Zé Delivery"}
                                """.formatted(categoriaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoriaNome").value("Delivery"));

        // A regra nova também resolveu a outra pendente da mesma importação
        mockMvc.perform(get("/transacoes").param("pendentes", "true").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.importacaoId == '%s')]", importacaoId).isEmpty());

        // 2ª importação: a regra categoriza sozinha
        mockMvc.perform(multipart("/importacoes")
                        .file(arquivo("fatura-setembro.csv", "date,title,amount\n2026-09-04,ZE DELIVERY Pedido 2,28.50\n"))
                        .header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categorizadas").value(1))
                .andExpect(jsonPath("$.pendentes").value(0));

        // Desfazer a primeira importação remove suas transações
        mockMvc.perform(delete("/importacoes/" + importacaoId).header("Authorization", token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/transacoes/" + transacaoId).header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void importacaoRecusaFormatoDesconhecido() throws Exception {
        mockMvc.perform(multipart("/importacoes")
                        .file(arquivo("extrato.pdf", "qualquer coisa"))
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString(".ofx ou .csv")));
    }

    @Test
    void resumoSomaReceitasEDespesasDoPeriodo() throws Exception {
        criarTransacao("Salário outubro", "4000.00", "2025-10-05");
        criarTransacao("Aluguel", "-1500.00", "2025-10-10");
        criarTransacao("Fora do período", "-999.00", "2025-11-01");

        mockMvc.perform(get("/transacoes/resumo").header("Authorization", token)
                        .param("inicio", "2025-10-01").param("fim", "2025-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receitas").value(4000.00))
                .andExpect(jsonPath("$.despesas").value(1500.00))
                .andExpect(jsonPath("$.saldo").value(2500.00));
    }

    @Test
    void metaRegistraProgresso() throws Exception {
        String resposta = mockMvc.perform(post("/metas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "Reserva de emergência", "valorAlvo": 2000.00, "dataLimite": "2027-06-30"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.percentual").value(0))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String metaId = JsonPath.read(resposta, "$.id");

        mockMvc.perform(post("/metas/" + metaId + "/movimentos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\": 500.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentual").value(25.0))
                .andExpect(jsonPath("$.valorRestante").value(1500.00));
    }

    private String criarCategoria(String nome, String tipo) throws Exception {
        String resposta = mockMvc.perform(post("/categorias").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"%s\", \"tipo\": \"%s\"}".formatted(nome, tipo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(resposta, "$.id");
    }

    private void criarTransacao(String descricao, String valor, String data) throws Exception {
        mockMvc.perform(post("/transacoes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\": \"%s\", \"valor\": %s, \"data\": \"%s\"}"
                                .formatted(descricao, valor, data)))
                .andExpect(status().isCreated());
    }

    private static MockMultipartFile arquivo(String nome, String conteudo) {
        return new MockMultipartFile("arquivo", nome, "text/plain", conteudo.getBytes(StandardCharsets.UTF_8));
    }
}
