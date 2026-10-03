package com.carlos.financasdescomplicadas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Limite de 72: o BCrypt ignora o que passa disso
public record NovaSenhaDTO(@NotBlank @Size(min = 8, max = 72) String senha) {}
