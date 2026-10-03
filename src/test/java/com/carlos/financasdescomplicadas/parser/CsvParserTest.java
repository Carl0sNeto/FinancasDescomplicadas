package com.carlos.financasdescomplicadas.parser;

import com.carlos.financasdescomplicadas.dto.TransacaoBruta;
import com.carlos.financasdescomplicadas.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvParserTest {

    private final CsvParser parser = new CsvParser();

    @Test
    void suportaApenasArquivosCsv() {
        assertThat(parser.suporta("extrato.CSV")).isTrue();
        assertThat(parser.suporta("extrato.ofx")).isFalse();
        assertThat(parser.suporta(null)).isFalse();
    }

    @Test
    void leFaturaDoCartaoNubankInvertendoOSinal() {
        String csv = """
                date,title,amount
                2026-09-03,Ifood *Restaurante,45.90
                2026-09-05,"Loja, com vírgula",120.00
                2026-09-10,Pagamento recebido,-500.00
                """;

        List<TransacaoBruta> transacoes = parse(csv, StandardCharsets.UTF_8);

        assertThat(transacoes).hasSize(3);
        assertThat(transacoes.get(0).data()).isEqualTo(LocalDate.of(2026, 9, 3));
        assertThat(transacoes.get(0).valor()).isEqualByComparingTo("-45.90");
        assertThat(transacoes.get(1).descricao()).isEqualTo("Loja, com vírgula");
        assertThat(transacoes.get(2).valor()).isEqualByComparingTo("500.00");
    }

    @Test
    void leExtratoDaContaNubankMantendoOSinal() {
        String csv = """
                Data,Valor,Identificador,Descrição
                01/09/2026,3500.00,abc-1,Transferência recebida - Empresa
                02/09/2026,-89.90,abc-2,Compra no débito - Mercado
                """;

        List<TransacaoBruta> transacoes = parse(csv, StandardCharsets.UTF_8);

        assertThat(transacoes).hasSize(2);
        assertThat(transacoes.get(0).data()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(transacoes.get(0).valor()).isEqualByComparingTo("3500.00");
        assertThat(transacoes.get(1).descricao()).isEqualTo("Compra no débito - Mercado");
        assertThat(transacoes.get(1).valor()).isEqualByComparingTo("-89.90");
    }

    @Test
    void aceitaPontoEVirgulaValorBrasileiroEArquivoEmWindows1252() {
        String csv = "Data;Descrição;Valor\n15/09/2026;Farmácia;-1.234,56\n";

        List<TransacaoBruta> transacoes = parse(csv, Charset.forName("windows-1252"));

        assertThat(transacoes).singleElement().satisfies(t -> {
            assertThat(t.descricao()).isEqualTo("Farmácia");
            assertThat(t.valor()).isEqualByComparingTo("-1234.56");
        });
    }

    @Test
    void recusaCabecalhoDesconhecido() {
        assertThatThrownBy(() -> parse("coluna1,coluna2\nx,y\n", StandardCharsets.UTF_8))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Layout de CSV");
    }

    @Test
    void indicaALinhaComValorInvalido() {
        String csv = "date,title,amount\n2026-09-03,Mercado,abc\n";

        assertThatThrownBy(() -> parse(csv, StandardCharsets.UTF_8))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Linha 2");
    }

    private List<TransacaoBruta> parse(String conteudo, Charset charset) {
        return parser.parse(new ByteArrayInputStream(conteudo.getBytes(charset)));
    }
}
