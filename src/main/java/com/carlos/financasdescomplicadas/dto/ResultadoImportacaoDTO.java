package com.carlos.financasdescomplicadas.dto;

public record ResultadoImportacaoDTO(
        ImportacaoResponseDTO importacao,
        int categorizadas,
        int pendentes
) {}
