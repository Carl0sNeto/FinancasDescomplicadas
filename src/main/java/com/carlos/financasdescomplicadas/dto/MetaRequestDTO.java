package com.carlos.financasdescomplicadas.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MetaRequestDTO(
        @NotBlank @Size(max = 100) String titulo,
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal valorAlvo,
        @PositiveOrZero @Digits(integer = 13, fraction = 2) BigDecimal valorAtual,
        LocalDate dataLimite
) {}
