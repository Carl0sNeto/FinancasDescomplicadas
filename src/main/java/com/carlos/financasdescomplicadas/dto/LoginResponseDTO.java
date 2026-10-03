package com.carlos.financasdescomplicadas.dto;

import com.carlos.financasdescomplicadas.model.Papel;

public record LoginResponseDTO(
        String token,
        String tipo,
        String nome,
        String email,
        Papel papel
) {
    public LoginResponseDTO(String token, String nome, String email, Papel papel) {
        this(token, "Bearer", nome, email, papel);
    }
}
