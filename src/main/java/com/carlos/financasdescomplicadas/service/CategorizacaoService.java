package com.carlos.financasdescomplicadas.service;

import com.carlos.financasdescomplicadas.dto.RegraResponseDTO;
import com.carlos.financasdescomplicadas.exception.RecursoNaoEncontradoException;
import com.carlos.financasdescomplicadas.exception.RegraNegocioException;
import com.carlos.financasdescomplicadas.model.Categoria;
import com.carlos.financasdescomplicadas.model.RegraCategorizacao;
import com.carlos.financasdescomplicadas.model.TipoCategoria;
import com.carlos.financasdescomplicadas.repository.RegraCategorizacaoRepository;
import com.carlos.financasdescomplicadas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Motor de categorização automática: compara a descrição da transação com as
 * palavras-chave salvas pelo usuário (ver docs/04-importacao-automatica.md).
 */
@Service
public class CategorizacaoService {

    private final RegraCategorizacaoRepository regraRepository;
    private final UsuarioRepository usuarioRepository;

    public CategorizacaoService(RegraCategorizacaoRepository regraRepository,
                                UsuarioRepository usuarioRepository) {
        this.regraRepository = regraRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<RegraCategorizacao> carregarRegras(UUID usuarioId) {
        return regraRepository.findByUsuarioIdOrderByPalavraChaveAsc(usuarioId);
    }

    /**
     * Procura a regra cuja palavra-chave aparece na descrição. Se mais de uma bater,
     * vence a palavra-chave mais longa (a mais específica). Regras cuja categoria não
     * combina com o sinal do valor são ignoradas.
     */
    public Optional<Categoria> sugerirCategoria(String descricao, BigDecimal valor,
                                                List<RegraCategorizacao> regras) {
        String descricaoNormalizada = normalizar(descricao);
        TipoCategoria tipo = TipoCategoria.doValor(valor);

        return regras.stream()
                .filter(regra -> regra.getCategoria().getTipo() == tipo)
                .filter(regra -> descricaoNormalizada.contains(regra.getPalavraChave()))
                .max(Comparator.comparingInt(regra -> regra.getPalavraChave().length()))
                .map(RegraCategorizacao::getCategoria);
    }

    /**
     * Cria (ou atualiza, se a palavra-chave já existir) uma regra a partir de uma
     * correção manual do usuário.
     */
    @Transactional
    public RegraCategorizacao aprenderRegra(UUID usuarioId, Categoria categoria, String palavraChave) {
        String chave = normalizar(palavraChave);
        if (chave.isEmpty()) {
            throw new RegraNegocioException("Palavra-chave da regra não pode ser vazia");
        }

        RegraCategorizacao regra = regraRepository.findByUsuarioIdAndPalavraChave(usuarioId, chave)
                .orElseGet(() -> new RegraCategorizacao(
                        usuarioRepository.getReferenceById(usuarioId), categoria, chave));
        regra.setCategoria(categoria);
        return regraRepository.save(regra);
    }

    @Transactional(readOnly = true)
    public List<RegraResponseDTO> listar(UUID usuarioId) {
        return carregarRegras(usuarioId).stream().map(RegraResponseDTO::de).toList();
    }

    @Transactional
    public void excluir(UUID usuarioId, UUID regraId) {
        RegraCategorizacao regra = regraRepository.findByIdAndUsuarioId(regraId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Regra"));
        regraRepository.delete(regra);
    }

    // Minúsculas, sem acentos e com espaços colapsados: "PADARIA  São João" -> "padaria sao joao"
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
