package com.carlos.financasdescomplicadas.dto;

import com.carlos.financasdescomplicadas.model.Papel;
import com.carlos.financasdescomplicadas.model.Usuario;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioResponseDTO(UUID id, String nome, String email, Papel papel, LocalDateTime criadoEm) {

    public static UsuarioResponseDTO de(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPapel(),
                usuario.getCriadoEm()
        );
    }
}
