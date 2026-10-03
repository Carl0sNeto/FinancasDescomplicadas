package com.carlos.financasdescomplicadas.controller;

import com.carlos.financasdescomplicadas.dto.LoginRequestDTO;
import com.carlos.financasdescomplicadas.dto.LoginResponseDTO;
import com.carlos.financasdescomplicadas.model.Usuario;
import com.carlos.financasdescomplicadas.repository.UsuarioRepository;
import com.carlos.financasdescomplicadas.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;

    public AuthController(AuthenticationManager authenticationManager,
                           JwtTokenProvider jwtTokenProvider,
                           UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO requestDTO) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(requestDTO.email(), requestDTO.senha())
        );

        String token = jwtTokenProvider.gerarToken(authentication);

        Usuario usuario = usuarioRepository.findByEmail(requestDTO.email())
                .orElseThrow();

        return ResponseEntity.ok(new LoginResponseDTO(
                token, usuario.getNome(), usuario.getEmail(), usuario.getPapel()));
    }
}
