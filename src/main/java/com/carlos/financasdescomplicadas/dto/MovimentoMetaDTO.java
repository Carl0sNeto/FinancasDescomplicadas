package com.carlos.financasdescomplicadas.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// Valor positivo guarda dinheiro na meta, negativo retira
public record MovimentoMetaDTO(
        @NotNull @Digits(integer = 13, fraction = 2) BigDecimal valor
) {}
