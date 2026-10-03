package com.carlos.financasdescomplicadas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MetaResponseDTO(
        UUID id,
        String titulo,
        BigDecimal valorAlvo,
        BigDecimal valorAtual,
        LocalDate dataLimite,
        BigDecimal percentual,
        BigDecimal valorRestante,
        boolean concluida
) {}
