package com.carlos.financasdescomplicadas.parser;

import com.carlos.financasdescomplicadas.dto.TransacaoBruta;
import com.carlos.financasdescomplicadas.exception.RegraNegocioException;
import com.carlos.financasdescomplicadas.model.FormatoExtrato;
import com.carlos.financasdescomplicadas.service.CategorizacaoService;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lê extratos CSV no layout do Nubank, identificando as colunas pelo cabeçalho:
 * <ul>
 *   <li>Cartão de crédito: {@code date,title,amount} — data ISO e compras com valor
 *       positivo, então o sinal é invertido (compra vira despesa).</li>
 *   <li>Conta: {@code Data,Valor,Identificador,Descrição} — data dd/MM/yyyy e valor
 *       já com sinal.</li>
 * </ul>
 * Outros CSVs com colunas equivalentes (data, descrição, valor) também funcionam.
 */
@Component
public class CsvParser implements ExtratoParser {

    private static final Set<String> COLUNAS_DATA = Set.of("date", "data");
    private static final Set<String> COLUNAS_DESCRICAO =
            Set.of("title", "descricao", "description", "historico", "lancamento");
    private static final Set<String> COLUNAS_VALOR = Set.of("amount", "valor");

    private static final List<DateTimeFormatter> FORMATOS_DATA = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    @Override
    public boolean suporta(String nomeArquivo) {
        return nomeArquivo != null && nomeArquivo.toLowerCase(Locale.ROOT).endsWith(".csv");
    }

    @Override
    public FormatoExtrato formato() {
        return FormatoExtrato.CSV;
    }

    @Override
    public List<TransacaoBruta> parse(InputStream arquivo) {
        List<String> linhas = LeitorTexto.lerTexto(arquivo).lines()
                .filter(linha -> !linha.isBlank())
                .toList();
        if (linhas.isEmpty()) {
            throw new RegraNegocioException("O arquivo CSV está vazio");
        }

        char separador = detectarSeparador(linhas.get(0));
        Layout layout = Layout.doCabecalho(dividir(linhas.get(0), separador));

        List<TransacaoBruta> transacoes = new ArrayList<>();
        for (int i = 1; i < linhas.size(); i++) {
            transacoes.add(lerLinha(dividir(linhas.get(i), separador), layout, i + 1));
        }
        return transacoes;
    }

    private TransacaoBruta lerLinha(List<String> colunas, Layout layout, int numeroLinha) {
        try {
            LocalDate data = lerData(colunas.get(layout.data()));
            BigDecimal valor = LeitorTexto.lerValor(colunas.get(layout.valor()));
            String descricao = colunas.get(layout.descricao()).trim();
            if (layout.inverterSinal()) {
                valor = valor.negate();
            }
            return new TransacaoBruta(data, descricao.isEmpty() ? "Sem descrição" : descricao, valor);
        } catch (IndexOutOfBoundsException e) {
            throw new RegraNegocioException("Linha " + numeroLinha + " do CSV tem colunas faltando", e);
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new RegraNegocioException("Linha " + numeroLinha + " do CSV com data ou valor inválido", e);
        }
    }

    private static LocalDate lerData(String texto) {
        String valor = texto.trim();
        for (DateTimeFormatter formato : FORMATOS_DATA) {
            try {
                return LocalDate.parse(valor, formato);
            } catch (DateTimeParseException ignorada) {
                // tenta o próximo formato
            }
        }
        throw new DateTimeParseException("Data em formato desconhecido", valor, 0);
    }

    private static char detectarSeparador(String cabecalho) {
        long pontoEVirgula = cabecalho.chars().filter(c -> c == ';').count();
        long virgula = cabecalho.chars().filter(c -> c == ',').count();
        return pontoEVirgula > virgula ? ';' : ',';
    }

    // Divide uma linha CSV respeitando campos entre aspas (que podem conter o separador)
    static List<String> dividir(String linha, char separador) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean entreAspas = false;

        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (c == '"') {
                if (entreAspas && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                    atual.append('"');
                    i++;
                } else {
                    entreAspas = !entreAspas;
                }
            } else if (c == separador && !entreAspas) {
                campos.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        campos.add(atual.toString());
        return campos;
    }

    private record Layout(int data, int descricao, int valor, boolean inverterSinal) {

        static Layout doCabecalho(List<String> cabecalho) {
            List<String> nomes = cabecalho.stream().map(CategorizacaoService::normalizar).toList();

            int data = indice(nomes, COLUNAS_DATA);
            int descricao = indice(nomes, COLUNAS_DESCRICAO);
            int valor = indice(nomes, COLUNAS_VALOR);
            if (data < 0 || descricao < 0 || valor < 0) {
                throw new RegraNegocioException(
                        "Layout de CSV não reconhecido. O cabeçalho precisa ter colunas de data, "
                                + "descrição e valor (ex.: \"date,title,amount\" do Nubank)");
            }

            // Fatura do cartão Nubank: compras vêm positivas
            boolean cartaoNubank = "title".equals(nomes.get(descricao)) && "amount".equals(nomes.get(valor));
            return new Layout(data, descricao, valor, cartaoNubank);
        }

        private static int indice(List<String> nomes, Set<String> aceitos) {
            for (int i = 0; i < nomes.size(); i++) {
                if (aceitos.contains(nomes.get(i))) {
                    return i;
                }
            }
            return -1;
        }
    }
}
