package com.carlos.financasdescomplicadas.dto;

import com.carlos.financasdescomplicadas.model.RegraCategorizacao;

import java.util.UUID;

public record RegraResponseDTO(UUID id, String palavraChave, UUID categoriaId, String categoriaNome) {

    public static RegraResponseDTO de(RegraCategorizacao regra) {
        return new RegraResponseDTO(
                regra.getId(),
                regra.getPalavraChave(),
                regra.getCategoria().getId(),
                regra.getCategoria().getNome()
        );
    }
}
