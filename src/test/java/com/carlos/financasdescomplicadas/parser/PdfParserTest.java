package com.carlos.financasdescomplicadas.parser;

import com.carlos.financasdescomplicadas.dto.TransacaoBruta;
import com.carlos.financasdescomplicadas.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class PdfParserTest {

    private final PdfParser parser = new PdfParser();

    @Test
    void suportaApenasArquivosPdf() {
        assertThat(parser.suporta("extrato-setembro.PDF")).isTrue();
        assertThat(parser.suporta("extrato.csv")).isFalse();
    }

    @Test
    void leExtratoDeContaComIndicadorDebitoCreditoESaldo() {
        // Data só na primeira transação do dia; coluna de saldo à direita
        byte[] pdf = PdfTeste.gerar(List.of(
                "Banco Exemplo S.A. - Extrato de conta corrente",
                "Período: 01/09/2026 a 30/09/2026",
                "Data|Lançamento|Valor (R$)|Saldo (R$)",
                "01/09/2026|SALDO ANTERIOR||1.000,00",
                "02/09/2026|PIX RECEBIDO MARIA|250,00 C|1.250,00",
                "|COMPRA CARTAO PADARIA PÃO QUENTE|32,50 D|1.217,50",
                "05/09/2026|PAGTO CONTA LUZ|-129,90|1.087,60",
                "10/09/2026|TOTALPASS MENSALIDADE|89,90 D|997,70",
                "|SALDO DO DIA||997,70"));

        List<TransacaoBruta> transacoes = parse(pdf);

        assertThat(transacoes)
                .extracting(TransacaoBruta::data, TransacaoBruta::descricao, t -> t.valor().toPlainString())
                .containsExactly(
                        tuple(LocalDate.of(2026, 9, 2), "PIX RECEBIDO MARIA", "250.00"),
                        tuple(LocalDate.of(2026, 9, 2), "COMPRA CARTAO PADARIA PÃO QUENTE", "-32.50"),
                        tuple(LocalDate.of(2026, 9, 5), "PAGTO CONTA LUZ", "-129.90"),
                        tuple(LocalDate.of(2026, 9, 10), "TOTALPASS MENSALIDADE", "-89.90"));
    }

    @Test
    void extratoSemIndicadorDeSinalUsaPalavrasDaDescricao() {
        byte[] pdf = PdfTeste.gerar(List.of(
                "Extrato - setembro/2026",
                "15/09/2026|TRANSFERENCIA RECEBIDA JOAO|1.500,00",
                "16/09/2026|SALÁRIO EMPRESA X|4.200,00",
                "17/09/2026|SUPERMERCADO BOM PREÇO|312,45"));

        List<TransacaoBruta> transacoes = parse(pdf);

        assertThat(transacoes).extracting(t -> t.valor().toPlainString())
                .containsExactly("1500.00", "4200.00", "-312.45");
    }

    @Test
    void leFaturaDeCartaoComMesPorExtensoEVirandoOAno() {
        // Fatura de janeiro/2027 com compra de dezembro/2026
        byte[] pdf = PdfTeste.gerar(List.of(
                "Fatura do cartão de crédito",
                "Vencimento: 10/01/2027",
                "Total da fatura|R$ 64,65",
                "15 DEZ|IFOOD *RESTAURANTE|R$ 45,90",
                "05 JAN|UBER *TRIP|R$ 18,75",
                "08 JAN|PAGAMENTO RECEBIDO|-R$ 500,00"));

        List<TransacaoBruta> transacoes = parse(pdf);

        assertThat(transacoes)
                .extracting(TransacaoBruta::data, TransacaoBruta::descricao, t -> t.valor().toPlainString())
                .containsExactly(
                        tuple(LocalDate.of(2026, 12, 15), "IFOOD *RESTAURANTE", "-45.90"),
                        tuple(LocalDate.of(2027, 1, 5), "UBER *TRIP", "-18.75"),
                        tuple(LocalDate.of(2027, 1, 8), "PAGAMENTO RECEBIDO", "500.00"));
    }

    @Test
    void pdfProtegidoPedeASenhaERecusaSenhaErrada() {
        byte[] pdf = PdfTeste.gerar(List.of("Extrato", "02/09/2026|MERCADO|50,00 D"), "12345");

        assertThatThrownBy(() -> parse(pdf))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("protegido por senha");
        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(pdf), "00000"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Senha do PDF incorreta");

        assertThat(parser.parse(new ByteArrayInputStream(pdf), "12345"))
                .singleElement()
                .satisfies(t -> assertThat(t.valor()).isEqualByComparingTo("-50.00"));
    }

    @Test
    void pdfSemTextoOuSemTransacoesExplicaOProblema() {
        assertThatThrownBy(() -> parse(PdfTeste.gerar(List.of())))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não tem texto");

        assertThatThrownBy(() -> parse(PdfTeste.gerar(List.of("Contrato de prestação de serviços", "Cláusula 1"))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("layout deste banco");
    }

    @Test
    void arquivoQueNaoEPdfDaErroAmigavel() {
        assertThatThrownBy(() -> parse("date,title,amount".getBytes()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Não foi possível ler o PDF");
    }

    private List<TransacaoBruta> parse(byte[] pdf) {
        return parser.parse(new ByteArrayInputStream(pdf));
    }
}
