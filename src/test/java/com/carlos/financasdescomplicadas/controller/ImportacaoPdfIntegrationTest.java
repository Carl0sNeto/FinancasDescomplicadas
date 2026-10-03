package com.carlos.financasdescomplicadas.controller;

import com.carlos.financasdescomplicadas.config.AjusteEsquemaBanco;
import com.carlos.financasdescomplicadas.parser.PdfTeste;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImportacaoPdfIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AjusteEsquemaBanco ajusteEsquemaBanco;

    private String token;

    @BeforeEach
    void login() throws Exception {
        String resposta = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"teste@financas.dev\", \"senha\": \"senha-de-teste\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        token = "Bearer " + JsonPath.read(resposta, "$.token");
    }

    @Test
    void importaPdfProtegidoInformandoASenha() throws Exception {
        byte[] pdf = PdfTeste.gerar(List.of(
                "Extrato de conta",
                "03/08/2026|PIX RECEBIDO CLIENTE|800,00 C",
                "04/08/2026|FARMACIA SAO JOAO|45,20 D"), "1234");
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "extrato-agosto.pdf", "application/pdf", pdf);

        mockMvc.perform(multipart("/importacoes").file(arquivo).header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("senha")));

        mockMvc.perform(multipart("/importacoes").file(arquivo).param("senha", "1234")
                        .header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importacao.formato").value("PDF"))
                .andExpect(jsonPath("$.importacao.totalTransacoes").value(2));
    }

    @Test
    void bancoAntigoComRestricaoSemPdfPassaAAceitarPdf() throws Exception {
        // Simula um banco criado antes do formato PDF (como o do Neon): restrição só com OFX e CSV
        removerRestricoesDeFormato();
        jdbc.execute("alter table importacao add constraint teste_formato_antigo check (formato in ('OFX', 'CSV'))");

        ajusteEsquemaBanco.run(null);

        assertThat(restricoesDeFormato()).noneMatch(clausula -> !clausula.contains("PDF"));
        importarPdfSimples();
    }

    @Test
    void bancoH2AntigoComColunaEnumSemPdfPassaAAceitarPdf() throws Exception {
        // No H2 o Hibernate usa tipo ENUM nativo; simula a coluna criada antes do formato PDF
        jdbc.update("delete from transacao where importacao_id in (select id from importacao where formato = 'PDF')");
        jdbc.update("delete from importacao where formato = 'PDF'");
        jdbc.execute("alter table importacao alter column formato set data type enum('OFX', 'CSV')");

        ajusteEsquemaBanco.run(null);

        importarPdfSimples();
    }

    private void importarPdfSimples() throws Exception {
        byte[] pdf = PdfTeste.gerar(List.of("Extrato", "10/08/2026|PADARIA|12,00 D"));
        mockMvc.perform(multipart("/importacoes")
                        .file(new MockMultipartFile("arquivo", "extrato.pdf", "application/pdf", pdf))
                        .header("Authorization", token))
                .andExpect(status().isCreated());
    }

    private List<String> restricoesDeFormato() {
        return jdbc.queryForList("""
                select cc.check_clause from information_schema.table_constraints tc
                join information_schema.check_constraints cc
                  on cc.constraint_name = tc.constraint_name and cc.constraint_schema = tc.constraint_schema
                where tc.constraint_type = 'CHECK' and lower(tc.table_name) = 'importacao'
                """, String.class).stream()
                .map(String::toUpperCase)
                .filter(c -> c.contains("'OFX'"))
                .toList();
    }

    private void removerRestricoesDeFormato() {
        jdbc.queryForList("""
                select tc.constraint_name, cc.check_clause from information_schema.table_constraints tc
                join information_schema.check_constraints cc
                  on cc.constraint_name = tc.constraint_name and cc.constraint_schema = tc.constraint_schema
                where tc.constraint_type = 'CHECK' and lower(tc.table_name) = 'importacao'
                """).stream()
                .filter(r -> r.get("check_clause").toString().toUpperCase().contains("'OFX'"))
                .forEach(r -> jdbc.execute("alter table importacao drop constraint \"" + r.get("constraint_name") + "\""));
    }
}
