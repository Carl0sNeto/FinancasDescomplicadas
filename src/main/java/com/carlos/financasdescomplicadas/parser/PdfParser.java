package com.carlos.financasdescomplicadas.parser;

import com.carlos.financasdescomplicadas.dto.TransacaoBruta;
import com.carlos.financasdescomplicadas.exception.RegraNegocioException;
import com.carlos.financasdescomplicadas.model.FormatoExtrato;
import com.carlos.financasdescomplicadas.service.CategorizacaoService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lê extratos e faturas em PDF a partir do texto do documento (PDFs escaneados, que são
 * só imagem, não funcionam).
 *
 * Cada banco monta o PDF de um jeito, então a leitura é por heurística, linha a linha:
 * uma transação é uma linha que começa com data ("03/09/2026", "03/09", "03 SET") e tem
 * um valor no formato brasileiro ("1.234,56"). O texto entre os dois é a descrição.
 *
 * O sinal do valor segue esta ordem:
 * <ol>
 *   <li>Indicador explícito: "-50,00", "50,00-" ou "50,00 D" = débito; "+50,00" ou "50,00 C" = crédito.</li>
 *   <li>Fatura de cartão: compras vêm sem sinal e créditos com "-", então o sinal é invertido.</li>
 *   <li>Extrato sem indicador: crédito se a descrição tiver palavras como "recebido" ou
 *       "salário"; senão, débito.</li>
 * </ol>
 * Se uma linha tiver dois valores, o segundo é tratado como saldo e ignorado.
 */
@Component
public class PdfParser implements ExtratoParser {

    private static final Map<String, Integer> MESES = Map.ofEntries(
            Map.entry("jan", 1), Map.entry("fev", 2), Map.entry("mar", 3), Map.entry("abr", 4),
            Map.entry("mai", 5), Map.entry("jun", 6), Map.entry("jul", 7), Map.entry("ago", 8),
            Map.entry("set", 9), Map.entry("out", 10), Map.entry("nov", 11), Map.entry("dez", 12));

    // Data no começo da linha: 03/09/2026, 03/09/26, 03/09, 03 SET, 03/SET
    private static final Pattern DATA_NO_INICIO = Pattern.compile(
            "^(\\d{1,2})\\s*[/.\\- ]\\s*(\\d{1,2}|jan|fev|mar|abr|mai|jun|jul|ago|set|out|nov|dez)"
                    + "(?:[/.\\-](\\d{4}|\\d{2}))?(?=\\s|$)",
            Pattern.CASE_INSENSITIVE);

    // Valor brasileiro com indicador de sinal opcional antes ou depois: -1.234,56 | 50,00 D | R$ 10,00
    private static final Pattern VALOR = Pattern.compile(
            "(?<![\\d,.])([-+−]\\s?)?(?:R\\$\\s?)?(\\d{1,3}(?:\\.\\d{3})+,\\d{2}|\\d+,\\d{2})(?![\\d,])"
                    + "(-(?!\\s?\\d)|\\s[DC](?![\\p{L}\\d]))?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DATA_COMPLETA = Pattern.compile("\\b(\\d{2})/(\\d{2})/(\\d{4})\\b");

    // Linhas que têm valor mas não são transações (comparado com a descrição normalizada)
    private static final Pattern LINHA_IGNORADA = Pattern.compile(
            "\\b(saldo|total|limite|vencimento|pagamento minimo|encargos)\\b");

    private static final List<String> PALAVRAS_DE_CREDITO = List.of(
            "receb", "credito", "deposito", "salario", "estorno", "rendimento", "reembolso", "devolucao");

    @Override
    public boolean suporta(String nomeArquivo) {
        return nomeArquivo != null && nomeArquivo.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    @Override
    public FormatoExtrato formato() {
        return FormatoExtrato.PDF;
    }

    @Override
    public List<TransacaoBruta> parse(InputStream arquivo) {
        return parse(arquivo, null);
    }

    @Override
    public List<TransacaoBruta> parse(InputStream arquivo, String senha) {
        String texto = extrairTexto(arquivo, senha);
        if (texto.isBlank()) {
            throw new RegraNegocioException("Este PDF não tem texto (provavelmente é uma imagem escaneada). "
                    + "Baixe o extrato original no app ou site do banco, ou exporte em OFX ou CSV");
        }

        String textoNormalizado = CategorizacaoService.normalizar(texto);
        boolean fatura = textoNormalizado.contains("fatura") && textoNormalizado.contains("vencimento")
                && !textoNormalizado.contains("extrato");
        LocalDate referencia = dataDeReferencia(texto);

        List<TransacaoBruta> transacoes = new ArrayList<>();
        // Muitos extratos só mostram a data na primeira transação do dia; as linhas
        // seguintes sem data herdam a última data vista
        LocalDate ultimaData = null;
        for (String bruta : texto.split("\\R")) {
            String linha = bruta.trim();
            LocalDate data = null;
            String resto = linha;

            Matcher inicio = DATA_NO_INICIO.matcher(linha);
            if (inicio.find()) {
                data = lerData(inicio, referencia);
                if (data != null) {
                    resto = linha.substring(inicio.end());
                    ultimaData = data;
                }
            }
            if (data == null) {
                data = ultimaData;
            }
            if (data == null) {
                continue;
            }

            TransacaoBruta transacao = lerTransacao(data, resto, fatura);
            if (transacao != null) {
                transacoes.add(transacao);
            }
        }

        if (transacoes.isEmpty()) {
            throw new RegraNegocioException("Não encontramos transações neste PDF: o layout deste banco "
                    + "ainda não é reconhecido. Se o banco oferecer, exporte o extrato em OFX ou CSV");
        }
        return transacoes;
    }

    private String extrairTexto(InputStream arquivo, String senha) {
        try (PDDocument documento = Loader.loadPDF(arquivo.readAllBytes(), senha == null ? "" : senha)) {
            PDFTextStripper stripper = new PDFTextStripper();
            // Ordena pela posição na página pra manter data, descrição e valor na mesma linha
            stripper.setSortByPosition(true);
            return stripper.getText(documento);
        } catch (InvalidPasswordException e) {
            throw new RegraNegocioException(senha == null || senha.isBlank()
                    ? "Este PDF é protegido por senha. Informe a senha do arquivo (muitos bancos usam parte do CPF)"
                    : "Senha do PDF incorreta", e);
        } catch (IOException e) {
            throw new RegraNegocioException("Não foi possível ler o PDF. Verifique se o arquivo não está corrompido", e);
        }
    }

    // "resto" é a linha sem a data: descrição seguida do valor (e às vezes do saldo)
    private static TransacaoBruta lerTransacao(LocalDate data, String resto, boolean fatura) {
        Matcher valor = VALOR.matcher(resto);
        if (!valor.find()) {
            return null;
        }
        String descricao = resto.substring(0, valor.start()).replace("R$", "").replaceAll("\\s+", " ").trim();
        String descricaoNormalizada = CategorizacaoService.normalizar(descricao);
        if (descricao.length() < 2 || LINHA_IGNORADA.matcher(descricaoNormalizada).find()) {
            return null;
        }

        BigDecimal quantia = LeitorTexto.lerValor(valor.group(2));
        return new TransacaoBruta(data, descricao, aplicarSinal(quantia, valor, fatura, descricaoNormalizada));
    }

    private static BigDecimal aplicarSinal(BigDecimal quantia, Matcher valor, boolean fatura, String descricao) {
        String antes = valor.group(1) == null ? "" : valor.group(1).trim();
        String depois = valor.group(3) == null ? "" : valor.group(3).trim().toUpperCase(Locale.ROOT);

        Boolean negativo = null;
        if (antes.equals("-") || antes.equals("−") || depois.equals("-") || depois.equals("D")) {
            negativo = true;
        } else if (antes.equals("+") || depois.equals("C")) {
            negativo = false;
        }

        if (fatura) {
            // Na fatura, compra (sem sinal) é despesa e crédito/pagamento (com "-") é receita
            return Boolean.TRUE.equals(negativo) ? quantia : quantia.negate();
        }
        if (negativo == null) {
            negativo = PALAVRAS_DE_CREDITO.stream().noneMatch(descricao::contains);
        }
        return negativo ? quantia.negate() : quantia;
    }

    /**
     * Datas sem ano (comuns em faturas) usam o ano da data de referência; se o mês for
     * posterior ao da referência, a transação é do ano anterior (ex.: compra de dezembro
     * numa fatura de janeiro).
     */
    private static LocalDate lerData(Matcher data, LocalDate referencia) {
        try {
            int dia = Integer.parseInt(data.group(1));
            String mesTexto = data.group(2).toLowerCase(Locale.ROOT);
            int mes = MESES.containsKey(mesTexto) ? MESES.get(mesTexto) : Integer.parseInt(mesTexto);

            int ano;
            if (data.group(3) != null) {
                ano = Integer.parseInt(data.group(3));
                if (ano < 100) {
                    ano += 2000;
                }
            } else {
                ano = mes > referencia.getMonthValue() ? referencia.getYear() - 1 : referencia.getYear();
            }
            return LocalDate.of(ano, mes, dia);
        } catch (DateTimeException | NumberFormatException e) {
            return null;
        }
    }

    // A data completa mais recente do documento (período, vencimento ou emissão); sem nenhuma, hoje
    private static LocalDate dataDeReferencia(String texto) {
        LocalDate maisRecente = null;
        Matcher m = DATA_COMPLETA.matcher(texto);
        while (m.find()) {
            try {
                LocalDate d = LocalDate.of(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(1)));
                if (maisRecente == null || d.isAfter(maisRecente)) {
                    maisRecente = d;
                }
            } catch (DateTimeException ignorada) {
                // não é uma data válida
            }
        }
        return maisRecente != null ? maisRecente : LocalDate.now();
    }
}
