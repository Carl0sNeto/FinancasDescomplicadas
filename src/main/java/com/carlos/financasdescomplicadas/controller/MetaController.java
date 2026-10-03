package com.carlos.financasdescomplicadas.controller;

import com.carlos.financasdescomplicadas.dto.MetaRequestDTO;
import com.carlos.financasdescomplicadas.dto.MetaResponseDTO;
import com.carlos.financasdescomplicadas.dto.MovimentoMetaDTO;
import com.carlos.financasdescomplicadas.security.UsuarioAutenticado;
import com.carlos.financasdescomplicadas.service.MetaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/metas")
public class MetaController {

    private final MetaService metaService;
    private final UsuarioAutenticado usuarioAutenticado;

    public MetaController(MetaService metaService, UsuarioAutenticado usuarioAutenticado) {
        this.metaService = metaService;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @GetMapping
    public List<MetaResponseDTO> listar() {
        return metaService.listar(usuarioAutenticado.id());
    }

    @PostMapping
    public ResponseEntity<MetaResponseDTO> criar(@Valid @RequestBody MetaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(metaService.criar(usuarioAutenticado.id(), dto));
    }

    @PutMapping("/{id}")
    public MetaResponseDTO atualizar(@PathVariable UUID id, @Valid @RequestBody MetaRequestDTO dto) {
        return metaService.atualizar(usuarioAutenticado.id(), id, dto);
    }

    @PostMapping("/{id}/movimentos")
    public MetaResponseDTO movimentar(@PathVariable UUID id, @Valid @RequestBody MovimentoMetaDTO dto) {
        return metaService.movimentar(usuarioAutenticado.id(), id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        metaService.excluir(usuarioAutenticado.id(), id);
        return ResponseEntity.noContent().build();
    }
}
