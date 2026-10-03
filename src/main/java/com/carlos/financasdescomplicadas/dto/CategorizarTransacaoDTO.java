package com.carlos.financasdescomplicadas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Correção manual de categoria. Por padrão a correção vira uma regra de categorização
 * (lembrar = true), usando a palavra-chave informada ou, se vazia, a descrição da transação.
 */
public record CategorizarTransacaoDTO(
        @NotNull UUID categoriaId,
        Boolean lembrar,
        @Size(max = 150) String palavraChave
) {
    public boolean deveLembrar() {
        return lembrar == null || lembrar;
    }
}
