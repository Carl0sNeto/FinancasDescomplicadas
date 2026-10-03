package com.carlos.financasdescomplicadas.controller;

import com.carlos.financasdescomplicadas.dto.ImportacaoResponseDTO;
import com.carlos.financasdescomplicadas.dto.RegraResponseDTO;
import com.carlos.financasdescomplicadas.dto.ResultadoImportacaoDTO;
import com.carlos.financasdescomplicadas.exception.RegraNegocioException;
import com.carlos.financasdescomplicadas.security.UsuarioAutenticado;
import com.carlos.financasdescomplicadas.service.CategorizacaoService;
import com.carlos.financasdescomplicadas.service.ImportacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@RestController
public class ImportacaoController {

    private final ImportacaoService importacaoService;
    private final CategorizacaoService categorizacaoService;
    private final UsuarioAutenticado usuarioAutenticado;

    public ImportacaoController(ImportacaoService importacaoService,
                                CategorizacaoService categorizacaoService,
                                UsuarioAutenticado usuarioAutenticado) {
        this.importacaoService = importacaoService;
        this.categorizacaoService = categorizacaoService;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    // Upload multipart com o campo "arquivo" (.ofx ou .csv)
    @PostMapping(value = "/importacoes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResultadoImportacaoDTO> importar(@RequestParam("arquivo") MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw new RegraNegocioException("Envie um arquivo de extrato");
        }
        try (InputStream conteudo = arquivo.getInputStream()) {
            ResultadoImportacaoDTO resultado = importacaoService.importar(
                    usuarioAutenticado.id(), arquivo.getOriginalFilename(), conteudo);
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (IOException e) {
            throw new RegraNegocioException("Não foi possível ler o arquivo enviado", e);
        }
    }

    @GetMapping("/importacoes")
    public List<ImportacaoResponseDTO> listar() {
        return importacaoService.listar(usuarioAutenticado.id());
    }

    @DeleteMapping("/importacoes/{id}")
    public ResponseEntity<Void> desfazer(@PathVariable UUID id) {
        importacaoService.desfazer(usuarioAutenticado.id(), id);
        return ResponseEntity.noContent().build();
    }

    // Regras de categorização aprendidas com as correções manuais
    @GetMapping("/regras")
    public List<RegraResponseDTO> listarRegras() {
        return categorizacaoService.listar(usuarioAutenticado.id());
    }

    @DeleteMapping("/regras/{id}")
    public ResponseEntity<Void> excluirRegra(@PathVariable UUID id) {
        categorizacaoService.excluir(usuarioAutenticado.id(), id);
        return ResponseEntity.noContent().build();
    }
}
