package com.carlos.financasdescomplicadas.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// valor positivo = receita, negativo = despesa; categoriaId é opcional
public record TransacaoRequestDTO(
        @NotBlank @Size(max = 255) String descricao,
        @NotNull @Digits(integer = 13, fraction = 2) BigDecimal valor,
        @NotNull LocalDate data,
        UUID categoriaId
) {}
