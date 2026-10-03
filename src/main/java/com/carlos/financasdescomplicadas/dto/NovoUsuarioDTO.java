package com.carlos.financasdescomplicadas.dto;

import com.carlos.financasdescomplicadas.model.Papel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// papel é opcional: sem ele a conta é criada como USUARIO
public record NovoUsuarioDTO(
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 72) String senha,
        Papel papel
) {}
