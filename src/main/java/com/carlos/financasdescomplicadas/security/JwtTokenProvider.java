package com.carlos.financasdescomplicadas.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey chave;
    private final long validadeEmMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String segredo,
            @Value("${jwt.validade-ms:86400000}") long validadeEmMs
    ) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes());
        this.validadeEmMs = validadeEmMs;
    }

    public String gerarToken(Authentication authentication) {
        String usuarioId = authentication.getName();

        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + validadeEmMs);

        return Jwts.builder()
                .subject(usuarioId)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave)
                .compact();
    }

    public String getUsuarioIdDoToken(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean tokenValido(String token) {
        try {
            Jwts.parser().verifyWith(chave).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
