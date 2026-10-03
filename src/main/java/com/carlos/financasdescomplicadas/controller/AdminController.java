package com.carlos.financasdescomplicadas.controller;

import com.carlos.financasdescomplicadas.dto.NovaSenhaDTO;
import com.carlos.financasdescomplicadas.dto.NovoUsuarioDTO;
import com.carlos.financasdescomplicadas.dto.UsuarioResponseDTO;
import com.carlos.financasdescomplicadas.security.UsuarioAutenticado;
import com.carlos.financasdescomplicadas.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Rotas restritas a ADMIN pelo SecurityConfig (/admin/**)
@RestController
@RequestMapping("/admin/usuarios")
public class AdminController {

    private final UsuarioService usuarioService;
    private final UsuarioAutenticado usuarioAutenticado;

    public AdminController(UsuarioService usuarioService, UsuarioAutenticado usuarioAutenticado) {
        this.usuarioService = usuarioService;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @GetMapping
    public List<UsuarioResponseDTO> listar() {
        return usuarioService.listar();
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> criar(@Valid @RequestBody NovoUsuarioDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.criar(dto));
    }

    @PutMapping("/{id}/senha")
    public ResponseEntity<Void> trocarSenha(@PathVariable UUID id, @Valid @RequestBody NovaSenhaDTO dto) {
        usuarioService.trocarSenha(id, dto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        usuarioService.excluir(usuarioAutenticado.id(), id);
        return ResponseEntity.noContent().build();
    }
}
