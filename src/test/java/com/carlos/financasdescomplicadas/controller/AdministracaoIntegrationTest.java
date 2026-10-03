package com.carlos.financasdescomplicadas.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Página de administração: o usuário inicial (application-test.properties) é ADMIN
 * e cria as demais contas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdministracaoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String tokenAdmin;

    @BeforeEach
    void loginAdmin() throws Exception {
        tokenAdmin = login("teste@financas.dev", "senha-de-teste");
    }

    @Test
    void usuarioInicialEhAdministrador() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"teste@financas.dev\", \"senha\": \"senha-de-teste\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("ADMIN"));
    }

    @Test
    void adminCriaContaQueConsegueEntrarEJaTemCategorias() throws Exception {
        String email = emailUnico();
        criarUsuario("Maria", email, "senha-da-maria", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.papel").value("USUARIO"));

        String tokenMaria = login(email, "senha-da-maria");
        mockMvc.perform(get("/categorias").header("Authorization", tokenMaria))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8));
    }

    @Test
    void usuarioComumNaoAcessaAdministracao() throws Exception {
        String email = emailUnico();
        criarUsuario("João", email, "senha-do-joao", "USUARIO").andExpect(status().isCreated());
        String tokenJoao = login(email, "senha-do-joao");

        mockMvc.perform(get("/admin/usuarios").header("Authorization", tokenJoao))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Acesso restrito a administradores"));
        mockMvc.perform(get("/admin/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    void naoPermiteEmailRepetidoNemSenhaCurta() throws Exception {
        criarUsuario("Outro", "TESTE@financas.dev", "senha-qualquer", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("Já existe")));

        criarUsuario("Curta", emailUnico(), "1234567", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.senha").exists());
    }

    @Test
    void adminTrocaSenhaDeOutraConta() throws Exception {
        String email = emailUnico();
        String id = idDe(criarUsuario("Ana", email, "senha-antiga", null).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8));

        mockMvc.perform(put("/admin/usuarios/" + id + "/senha").header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senha\": \"senha-nova-123\"}"))
                .andExpect(status().isNoContent());

        loginEsperando(email, "senha-antiga", 401);
        login(email, "senha-nova-123");
    }

    @Test
    void excluirContaApagaOsDadosDelaMasNaoAPropria() throws Exception {
        String email = emailUnico();
        String id = idDe(criarUsuario("Pedro", email, "senha-do-pedro", null).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
        String tokenPedro = login(email, "senha-do-pedro");
        mockMvc.perform(post("/metas").header("Authorization", tokenPedro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Carro\", \"valorAlvo\": 30000}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/transacoes").header("Authorization", tokenPedro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\": \"Mercado\", \"valor\": -50, \"data\": \"2026-09-01\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/admin/usuarios/" + id).header("Authorization", tokenAdmin))
                .andExpect(status().isNoContent());
        loginEsperando(email, "senha-do-pedro", 401);
        // O token antigo deixa de valer, porque o usuário é recarregado a cada requisição
        mockMvc.perform(get("/metas").header("Authorization", tokenPedro)).andExpect(status().isUnauthorized());

        String lista = mockMvc.perform(get("/admin/usuarios").header("Authorization", tokenAdmin))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<String> meusIds = JsonPath.read(lista, "$[?(@.email == 'teste@financas.dev')].id");
        String meuId = meusIds.get(0);
        mockMvc.perform(delete("/admin/usuarios/" + meuId).header("Authorization", tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Você não pode excluir a própria conta"));
    }

    private org.springframework.test.web.servlet.ResultActions criarUsuario(
            String nome, String email, String senha, String papel) throws Exception {
        String corpoPapel = papel == null ? "" : ", \"papel\": \"%s\"".formatted(papel);
        return mockMvc.perform(post("/admin/usuarios").header("Authorization", tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\": \"%s\", \"email\": \"%s\", \"senha\": \"%s\"%s}"
                        .formatted(nome, email, senha, corpoPapel)));
    }

    private String login(String email, String senha) throws Exception {
        String resposta = loginEsperando(email, senha, 200);
        return "Bearer " + JsonPath.read(resposta, "$.token");
    }

    private String loginEsperando(String email, String senha, int status) throws Exception {
        return mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"senha\": \"%s\"}".formatted(email, senha)))
                .andExpect(status().is(status))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static String idDe(String json) {
        return JsonPath.read(json, "$.id");
    }

    private static String emailUnico() {
        return "conta-" + UUID.randomUUID().toString().substring(0, 8) + "@exemplo.dev";
    }
}
