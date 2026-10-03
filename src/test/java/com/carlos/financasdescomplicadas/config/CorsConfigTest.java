package com.carlos.financasdescomplicadas.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void mantemOrigemQueJaEstaCorreta() {
        assertThat(CorsConfig.normalizarOrigem("https://financas-web.onrender.com"))
                .isEqualTo("https://financas-web.onrender.com");
        assertThat(CorsConfig.normalizarOrigem("http://localhost:5500"))
                .isEqualTo("http://localhost:5500");
    }

    @Test
    void removeBarraFinalCaminhoEspacosEAspas() {
        assertThat(CorsConfig.normalizarOrigem("https://financas-web.onrender.com/"))
                .isEqualTo("https://financas-web.onrender.com");
        assertThat(CorsConfig.normalizarOrigem(" https://financas-web.onrender.com/index.html "))
                .isEqualTo("https://financas-web.onrender.com");
        assertThat(CorsConfig.normalizarOrigem("\"https://Financas-Web.onrender.com\""))
                .isEqualTo("https://financas-web.onrender.com");
    }
}
