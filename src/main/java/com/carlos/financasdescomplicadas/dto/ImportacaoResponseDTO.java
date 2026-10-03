package com.carlos.financasdescomplicadas.dto;

import com.carlos.financasdescomplicadas.model.FormatoExtrato;
import com.carlos.financasdescomplicadas.model.Importacao;

import java.time.LocalDateTime;
import java.util.UUID;

public record ImportacaoResponseDTO(
        UUID id,
        String nomeArquivo,
        FormatoExtrato formato,
        LocalDateTime importadoEm,
        int totalTransacoes
) {
    public static ImportacaoResponseDTO de(Importacao importacao) {
        return new ImportacaoResponseDTO(
                importacao.getId(),
                importacao.getNomeArquivo(),
                importacao.getFormato(),
                importacao.getImportadoEm(),
                importacao.getTotalTransacoes()
        );
    }
}
