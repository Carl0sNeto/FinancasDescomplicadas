package com.carlos.financasdescomplicadas.parser;

import com.carlos.financasdescomplicadas.exception.RegraNegocioException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

// Utilitários compartilhados pelos parsers de extrato
final class LeitorTexto {

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private LeitorTexto() {
    }

    /**
     * Bancos brasileiros exportam ora em UTF-8, ora em Windows-1252 (Latin-1).
     * Tenta UTF-8 estrito e, se os bytes não forem válidos, cai pro Windows-1252.
     */
    static String lerTexto(InputStream arquivo) {
        byte[] bytes;
        try {
            bytes = arquivo.readAllBytes();
        } catch (IOException e) {
            throw new RegraNegocioException("Não foi possível ler o arquivo enviado", e);
        }

        String texto;
        try {
            texto = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            texto = new String(bytes, WINDOWS_1252);
        }
        return texto.startsWith("﻿") ? texto.substring(1) : texto;
    }

    /**
     * Converte valores nos formatos "1234.56", "-1.234,56", "R$ 50,00" etc.
     * Com vírgula presente, ela é o separador decimal e os pontos são de milhar.
     */
    static BigDecimal lerValor(String bruto) {
        String valor = bruto.replace("R$", "").replace(" ", "").replace(" ", "").trim();
        if (valor.contains(",")) {
            valor = valor.replace(".", "").replace(",", ".");
        }
        if (valor.startsWith("+")) {
            valor = valor.substring(1);
        }
        return new BigDecimal(valor);
    }
}
