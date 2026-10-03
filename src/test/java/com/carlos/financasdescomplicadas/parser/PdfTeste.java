package com.carlos.financasdescomplicadas.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Gera PDFs de extrato pros testes. Cada linha é dividida em colunas por "|", e cada
 * coluna é escrita numa posição horizontal diferente, como nos extratos reais.
 */
public final class PdfTeste {

    private static final float[] COLUNAS_X = {40, 110, 400, 490};

    private PdfTeste() {
    }

    public static byte[] gerar(List<String> linhas) {
        return gerar(linhas, null);
    }

    public static byte[] gerar(List<String> linhas, String senha) {
        try (PDDocument documento = new PDDocument()) {
            PDPage pagina = new PDPage();
            documento.addPage(pagina);
            PDType1Font fonte = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream conteudo = new PDPageContentStream(documento, pagina)) {
                float y = 750;
                for (String linha : linhas) {
                    String[] colunas = linha.split("\\|");
                    for (int i = 0; i < colunas.length; i++) {
                        if (colunas[i].isBlank()) {
                            continue;
                        }
                        conteudo.beginText();
                        conteudo.setFont(fonte, 10);
                        conteudo.newLineAtOffset(COLUNAS_X[Math.min(i, COLUNAS_X.length - 1)], y);
                        conteudo.showText(colunas[i].trim());
                        conteudo.endText();
                    }
                    y -= 16;
                }
            }

            if (senha != null) {
                StandardProtectionPolicy protecao =
                        new StandardProtectionPolicy("dono-" + senha, senha, new AccessPermission());
                protecao.setEncryptionKeyLength(128);
                documento.protect(protecao);
            }

            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            documento.save(saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
