package com.carlos.financasdescomplicadas.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Backend e frontend ficam em domínios diferentes (ver docs/02-arquitetura.md),
 * então o navegador só aceita as respostas da API se a origem do frontend estiver liberada.
 * As origens vêm de app.cors.origens (variável de ambiente CORS_ORIGENS), separadas por vírgula.
 */
@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.origens}") List<String> origens) {
        List<String> origensLiberadas = origens.stream()
                .map(CorsConfig::normalizarOrigem)
                .filter(o -> !o.isEmpty())
                .distinct()
                .toList();
        log.info("CORS liberado para: {}", origensLiberadas);

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origensLiberadas);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * O navegador compara a origem exatamente (esquema + host + porta). Aceita valores colados
     * com aspas, barra no final ou caminho ("https://site.onrender.com/index.html") e deixa
     * só a origem: "https://site.onrender.com".
     */
    static String normalizarOrigem(String valor) {
        String origem = valor.trim().replaceAll("^[\"']+|[\"']+$", "").trim();
        int inicioHost = origem.indexOf("://");
        if (inicioHost >= 0) {
            int inicioCaminho = origem.indexOf('/', inicioHost + 3);
            if (inicioCaminho >= 0) {
                origem = origem.substring(0, inicioCaminho);
            }
        }
        return origem.toLowerCase();
    }
}
