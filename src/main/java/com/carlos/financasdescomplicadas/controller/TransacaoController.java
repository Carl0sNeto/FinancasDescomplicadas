package com.carlos.financasdescomplicadas.controller;

import com.carlos.financasdescomplicadas.dto.CategorizarTransacaoDTO;
import com.carlos.financasdescomplicadas.dto.ResumoDTO;
import com.carlos.financasdescomplicadas.dto.TransacaoRequestDTO;
import com.carlos.financasdescomplicadas.dto.TransacaoResponseDTO;
import com.carlos.financasdescomplicadas.security.UsuarioAutenticado;
import com.carlos.financasdescomplicadas.service.TransacaoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transacoes")
public class TransacaoController {

    private final TransacaoService transacaoService;
    private final UsuarioAutenticado usuarioAutenticado;

    public TransacaoController(TransacaoService transacaoService, UsuarioAutenticado usuarioAutenticado) {
        this.transacaoService = transacaoService;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    // Ex.: GET /transacoes?inicio=2026-10-01&fim=2026-10-31&categoriaId=... ou ?pendentes=true
    @GetMapping
    public List<TransacaoResponseDTO> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(defaultValue = "false") boolean pendentes) {
        return transacaoService.listar(usuarioAutenticado.id(), inicio, fim, categoriaId, pendentes);
    }

    // Totais do período (padrão: mês atual) e despesas por categoria, pro dashboard
    @GetMapping("/resumo")
    public ResumoDTO resumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return transacaoService.resumo(usuarioAutenticado.id(), inicio, fim);
    }

    @GetMapping("/{id}")
    public TransacaoResponseDTO buscar(@PathVariable UUID id) {
        return transacaoService.buscar(usuarioAutenticado.id(), id);
    }

    @PostMapping
    public ResponseEntity<TransacaoResponseDTO> criar(@Valid @RequestBody TransacaoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transacaoService.criar(usuarioAutenticado.id(), dto));
    }

    @PutMapping("/{id}")
    public TransacaoResponseDTO atualizar(@PathVariable UUID id, @Valid @RequestBody TransacaoRequestDTO dto) {
        return transacaoService.atualizar(usuarioAutenticado.id(), id, dto);
    }

    @PatchMapping("/{id}/categoria")
    public TransacaoResponseDTO categorizar(@PathVariable UUID id,
                                            @Valid @RequestBody CategorizarTransacaoDTO dto) {
        return transacaoService.categorizar(usuarioAutenticado.id(), id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        transacaoService.excluir(usuarioAutenticado.id(), id);
        return ResponseEntity.noContent().build();
    }
}
