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

class OfxParserTest {

    private final OfxParser parser = new OfxParser();

    @Test
    void leOfxSgmlSemTagsDeFechamento() {
        // Formato OFX 1.x, comum nos bancos brasileiros
        String ofx = """
                OFXHEADER:100
                DATA:OFXSGML
                VERSION:102
                CHARSET:1252

                <OFX>
                <BANKMSGSRSV1><STMTTRNRS><STMTRS>
                <BANKTRANLIST>
                <DTSTART>20260901
                <STMTTRN>
                <TRNTYPE>DEBIT
                <DTPOSTED>20260903120000[-3:BRT]
                <TRNAMT>-45.90
                <FITID>1
                <MEMO>Compra no débito - Padaria Pão &amp; Cia
                </STMTTRN>
                <STMTTRN>
                <TRNTYPE>CREDIT
                <DTPOSTED>20260905
                <TRNAMT>3500,00
                <FITID>2
                <NAME>SALARIO EMPRESA
                </STMTTRN>
                </BANKTRANLIST>
                </STMTRS></STMTTRNRS></BANKMSGSRSV1>
                </OFX>
                """;

        List<TransacaoBruta> transacoes = parse(ofx, Charset.forName("windows-1252"));

        assertThat(transacoes).hasSize(2);
        assertThat(transacoes.get(0).data()).isEqualTo(LocalDate.of(2026, 9, 3));
        assertThat(transacoes.get(0).valor()).isEqualByComparingTo("-45.90");
        assertThat(transacoes.get(0).descricao()).isEqualTo("Compra no débito - Padaria Pão & Cia");
        assertThat(transacoes.get(1).descricao()).isEqualTo("SALARIO EMPRESA");
        assertThat(transacoes.get(1).valor()).isEqualByComparingTo("3500.00");
    }

    @Test
    void leOfxXmlComTagsDeFechamento() {
        String ofx = """
                <?xml version="1.0" encoding="UTF-8"?>
                <OFX><BANKMSGSRSV1><STMTTRNRS><STMTRS><BANKTRANLIST>
                <STMTTRN><TRNTYPE>DEBIT</TRNTYPE><DTPOSTED>20260910</DTPOSTED><TRNAMT>-12.50</TRNAMT><MEMO>Uber</MEMO></STMTTRN>
                </BANKTRANLIST></STMTRS></STMTTRNRS></BANKMSGSRSV1></OFX>
                """;

        List<TransacaoBruta> transacoes = parse(ofx, StandardCharsets.UTF_8);

        assertThat(transacoes).singleElement().satisfies(t -> {
            assertThat(t.descricao()).isEqualTo("Uber");
            assertThat(t.valor()).isEqualByComparingTo("-12.50");
            assertThat(t.data()).isEqualTo(LocalDate.of(2026, 9, 10));
        });
    }

    @Test
    void recusaArquivoQueNaoEOfx() {
        assertThatThrownBy(() -> parse("date,title,amount", StandardCharsets.UTF_8))
                .isInstanceOf(RegraNegocioException.class);
    }

    private List<TransacaoBruta> parse(String conteudo, Charset charset) {
        return parser.parse(new ByteArrayInputStream(conteudo.getBytes(charset)));
    }
}
