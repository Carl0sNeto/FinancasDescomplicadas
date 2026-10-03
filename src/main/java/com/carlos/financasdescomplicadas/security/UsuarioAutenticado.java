package com.carlos.financasdescomplicadas.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Dá aos controllers o id do usuário logado. O JwtAuthFilter coloca no SecurityContext
 * um UserDetails cujo username é o id (uuid) do usuário.
 */
@Component
public class UsuarioAutenticado {

    public UUID id() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof UserDetails usuario)) {
            throw new AuthenticationCredentialsNotFoundException("Usuário não autenticado");
        }
        return UUID.fromString(usuario.getUsername());
    }
}
