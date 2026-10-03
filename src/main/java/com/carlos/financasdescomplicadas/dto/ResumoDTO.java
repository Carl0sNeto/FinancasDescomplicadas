package com.carlos.financasdescomplicadas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ResumoDTO(
        LocalDate inicio,
        LocalDate fim,
        BigDecimal receitas,
        BigDecimal despesas,
        BigDecimal saldo,
        long pendentes,
        List<TotalPorCategoriaDTO> despesasPorCategoria
) {}
