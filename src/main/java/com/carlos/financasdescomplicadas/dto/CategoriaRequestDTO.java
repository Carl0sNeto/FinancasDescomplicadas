package com.carlos.financasdescomplicadas.dto;

import com.carlos.financasdescomplicadas.model.TipoCategoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoriaRequestDTO(
        @NotBlank @Size(max = 60) String nome,
        @NotNull TipoCategoria tipo
) {}
