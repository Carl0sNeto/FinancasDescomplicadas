package com.carlos.financasdescomplicadas.dto;

import java.math.BigDecimal;
import java.util.UUID;

// categoriaId e categoriaNome nulos = transações ainda sem categoria
public record TotalPorCategoriaDTO(UUID categoriaId, String categoriaNome, BigDecimal total) {}
