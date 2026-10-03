package com.carlos.financasdescomplicadas.controller;

import com.carlos.financasdescomplicadas.dto.CategoriaRequestDTO;
import com.carlos.financasdescomplicadas.dto.CategoriaResponseDTO;
import com.carlos.financasdescomplicadas.security.UsuarioAutenticado;
import com.carlos.financasdescomplicadas.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;
    private final UsuarioAutenticado usuarioAutenticado;

    public CategoriaController(CategoriaService categoriaService, UsuarioAutenticado usuarioAutenticado) {
        this.categoriaService = categoriaService;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @GetMapping
    public List<CategoriaResponseDTO> listar() {
        return categoriaService.listar(usuarioAutenticado.id());
    }

    @PostMapping
    public ResponseEntity<CategoriaResponseDTO> criar(@Valid @RequestBody CategoriaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoriaService.criar(usuarioAutenticado.id(), dto));
    }

    @PutMapping("/{id}")
    public CategoriaResponseDTO atualizar(@PathVariable UUID id, @Valid @RequestBody CategoriaRequestDTO dto) {
        return categoriaService.atualizar(usuarioAutenticado.id(), id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        categoriaService.excluir(usuarioAutenticado.id(), id);
        return ResponseEntity.noContent().build();
    }
}
