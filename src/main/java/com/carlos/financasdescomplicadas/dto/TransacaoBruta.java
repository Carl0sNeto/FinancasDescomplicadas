package com.carlos.financasdescomplicadas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// Resultado cru do parser de extrato, antes de virar uma Transacao
public record TransacaoBruta(LocalDate data, String descricao, BigDecimal valor) {}
