package com.carlos.financasdescomplicadas.dto;

public record LoginResponseDTO(
        String token,
        String tipo,
        String nome,
        String email
) {
    public LoginResponseDTO(String token, String nome, String email) {
        this(token, "Bearer", nome, email);
    }
}
