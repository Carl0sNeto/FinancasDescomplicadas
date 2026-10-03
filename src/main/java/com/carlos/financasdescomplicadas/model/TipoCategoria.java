package com.carlos.financasdescomplicadas.model;

import java.math.BigDecimal;

public enum TipoCategoria {
    RECEITA,
    DESPESA;

    // O tipo da transação vem do sinal do valor: positivo = receita, negativo = despesa
    public static TipoCategoria doValor(BigDecimal valor) {
        return valor.signum() >= 0 ? RECEITA : DESPESA;
    }
}
