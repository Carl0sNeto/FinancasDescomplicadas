package com.carlos.financasdescomplicadas.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ErroResponseDTO(
        int status,
        String erro,
        String mensagem,
        Map<String, String> campos,
        LocalDateTime timestamp
) {
    public ErroResponseDTO(int status, String erro, String mensagem) {
        this(status, erro, mensagem, null, LocalDateTime.now());
    }
}
