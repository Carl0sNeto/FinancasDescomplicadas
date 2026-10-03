package com.carlos.financasdescomplicadas.parser;

import com.carlos.financasdescomplicadas.dto.TransacaoBruta;
import com.carlos.financasdescomplicadas.exception.RegraNegocioException;
import com.carlos.financasdescomplicadas.model.FormatoExtrato;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lê extratos OFX (1.x em SGML e 2.x em XML).
 *
 * Em vez de uma lib como a ofx4j, faz uma leitura direta dos blocos {@code <STMTTRN>}:
 * no OFX 1.x as tags folha não têm fechamento, e muitos bancos brasileiros geram
 * arquivos que fogem um pouco da especificação — ler só as tags que importam
 * (DTPOSTED, TRNAMT, MEMO/NAME) é mais tolerante.
 */
@Component
public class OfxParser implements ExtratoParser {

    private static final Pattern BLOCO_TRANSACAO = Pattern.compile(
            "<STMTTRN>(.*?)(?=</STMTTRN>|<STMTTRN>|</BANKTRANLIST>|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final DateTimeFormatter DATA_OFX = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public boolean suporta(String nomeArquivo) {
        return nomeArquivo != null && nomeArquivo.toLowerCase(Locale.ROOT).endsWith(".ofx");
    }

    @Override
    public FormatoExtrato formato() {
        return FormatoExtrato.OFX;
    }

    @Override
    public List<TransacaoBruta> parse(InputStream arquivo) {
        String conteudo = LeitorTexto.lerTexto(arquivo);
        if (!conteudo.toUpperCase(Locale.ROOT).contains("<OFX>")) {
            throw new RegraNegocioException("O arquivo não parece ser um extrato OFX válido");
        }

        List<TransacaoBruta> transacoes = new ArrayList<>();
        Matcher blocos = BLOCO_TRANSACAO.matcher(conteudo);
        while (blocos.find()) {
            transacoes.add(lerTransacao(blocos.group(1), transacoes.size() + 1));
        }
        return transacoes;
    }

    private TransacaoBruta lerTransacao(String bloco, int numero) {
        String dataBruta = tag(bloco, "DTPOSTED");
        String valorBruto = tag(bloco, "TRNAMT");
        String descricao = tag(bloco, "MEMO");
        if (descricao == null || descricao.isBlank()) {
            descricao = tag(bloco, "NAME");
        }

        if (dataBruta == null || valorBruto == null) {
            throw new RegraNegocioException("Transação " + numero + " do OFX sem data ou valor");
        }

        try {
            // DTPOSTED vem como yyyyMMdd[HHmmss[.XXX]][[-3:BRT]] — só a data interessa
            LocalDate data = LocalDate.parse(dataBruta.substring(0, 8), DATA_OFX);
            BigDecimal valor = LeitorTexto.lerValor(valorBruto);
            String texto = descricao == null || descricao.isBlank() ? "Sem descrição" : descricao;
            return new TransacaoBruta(data, texto, valor);
        } catch (DateTimeParseException | NumberFormatException | StringIndexOutOfBoundsException e) {
            throw new RegraNegocioException("Transação " + numero + " do OFX com data ou valor inválido", e);
        }
    }

    // Valor de uma tag folha, com ou sem tag de fechamento (OFX 1.x x 2.x)
    private static String tag(String bloco, String nome) {
        Matcher m = Pattern.compile("<" + nome + ">([^<\\r\\n]*)", Pattern.CASE_INSENSITIVE).matcher(bloco);
        if (!m.find()) {
            return null;
        }
        return desescapar(m.group(1).trim());
    }

    private static String desescapar(String texto) {
        return texto.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
                .replaceAll("\\s+", " ");
    }
}
