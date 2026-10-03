package com.carlos.financasdescomplicadas.security;

import com.carlos.financasdescomplicadas.model.Usuario;
import com.carlos.financasdescomplicadas.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identificador) throws UsernameNotFoundException {
        // identificador é o email (no login) ou o id em uuid (nas requisições já autenticadas)
        Usuario usuario = buscarPorEmailOuId(identificador);

        return new User(
                usuario.getId().toString(),
                usuario.getSenhaHash(),
                Collections.emptyList()
        );
    }

    private Usuario buscarPorEmailOuId(String identificador) {
        try {
            UUID id = UUID.fromString(identificador);
            return usuarioRepository.findById(id)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        } catch (IllegalArgumentException e) {
            return usuarioRepository.findByEmail(identificador)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        }
    }
}
