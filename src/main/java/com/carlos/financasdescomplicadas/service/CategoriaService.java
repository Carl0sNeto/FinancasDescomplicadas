package com.carlos.financasdescomplicadas.service;

import com.carlos.financasdescomplicadas.dto.CategoriaRequestDTO;
import com.carlos.financasdescomplicadas.dto.CategoriaResponseDTO;
import com.carlos.financasdescomplicadas.exception.RecursoNaoEncontradoException;
import com.carlos.financasdescomplicadas.exception.RegraNegocioException;
import com.carlos.financasdescomplicadas.model.Categoria;
import com.carlos.financasdescomplicadas.model.TipoCategoria;
import com.carlos.financasdescomplicadas.model.Usuario;
import com.carlos.financasdescomplicadas.repository.CategoriaRepository;
import com.carlos.financasdescomplicadas.repository.RegraCategorizacaoRepository;
import com.carlos.financasdescomplicadas.repository.TransacaoRepository;
import com.carlos.financasdescomplicadas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CategoriaService {

    // Categorias criadas junto com um usuário novo, pra ele não começar do zero
    private static final Map<String, TipoCategoria> CATEGORIAS_PADRAO = Map.of(
            "Salário", TipoCategoria.RECEITA,
            "Outras receitas", TipoCategoria.RECEITA,
            "Alimentação", TipoCategoria.DESPESA,
            "Moradia", TipoCategoria.DESPESA,
            "Transporte", TipoCategoria.DESPESA,
            "Saúde", TipoCategoria.DESPESA,
            "Lazer", TipoCategoria.DESPESA,
            "Outras despesas", TipoCategoria.DESPESA
    );

    private final CategoriaRepository categoriaRepository;
    private final TransacaoRepository transacaoRepository;
    private final RegraCategorizacaoRepository regraRepository;
    private final UsuarioRepository usuarioRepository;

    public CategoriaService(CategoriaRepository categoriaRepository,
                            TransacaoRepository transacaoRepository,
                            RegraCategorizacaoRepository regraRepository,
                            UsuarioRepository usuarioRepository) {
        this.categoriaRepository = categoriaRepository;
        this.transacaoRepository = transacaoRepository;
        this.regraRepository = regraRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> listar(UUID usuarioId) {
        return categoriaRepository.findByUsuarioIdOrderByTipoAscNomeAsc(usuarioId).stream()
                .map(CategoriaResponseDTO::de)
                .toList();
    }

    @Transactional
    public CategoriaResponseDTO criar(UUID usuarioId, CategoriaRequestDTO dto) {
        String nome = dto.nome().trim();
        validarNomeDisponivel(usuarioId, nome, dto.tipo());

        Categoria categoria = new Categoria(usuarioRepository.getReferenceById(usuarioId), nome, dto.tipo());
        return CategoriaResponseDTO.de(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponseDTO atualizar(UUID usuarioId, UUID categoriaId, CategoriaRequestDTO dto) {
        Categoria categoria = buscarDoUsuario(usuarioId, categoriaId);
        String nome = dto.nome().trim();

        boolean mudouTipo = categoria.getTipo() != dto.tipo();
        if (mudouTipo && transacaoRepository.existsByCategoriaId(categoriaId)) {
            throw new RegraNegocioException(
                    "Não é possível mudar o tipo de uma categoria que já tem transações");
        }
        boolean mudouNome = !categoria.getNome().equalsIgnoreCase(nome);
        if (mudouNome || mudouTipo) {
            validarNomeDisponivel(usuarioId, nome, dto.tipo());
        }

        categoria.setNome(nome);
        categoria.setTipo(dto.tipo());
        return CategoriaResponseDTO.de(categoria);
    }

    /**
     * Excluir uma categoria não apaga transações: elas voltam a ficar pendentes de
     * categorização. As regras que apontavam pra ela são removidas.
     */
    @Transactional
    public void excluir(UUID usuarioId, UUID categoriaId) {
        Categoria categoria = buscarDoUsuario(usuarioId, categoriaId);
        transacaoRepository.descategorizarPorCategoria(categoriaId);
        regraRepository.deleteByCategoriaId(categoriaId);
        categoriaRepository.delete(categoria);
    }

    @Transactional(readOnly = true)
    public Categoria buscarDoUsuario(UUID usuarioId, UUID categoriaId) {
        return categoriaRepository.findByIdAndUsuarioId(categoriaId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria"));
    }

    @Transactional
    public void criarCategoriasPadrao(Usuario usuario) {
        CATEGORIAS_PADRAO.forEach((nome, tipo) ->
                categoriaRepository.save(new Categoria(usuario, nome, tipo)));
    }

    private void validarNomeDisponivel(UUID usuarioId, String nome, TipoCategoria tipo) {
        if (categoriaRepository.existsByUsuarioIdAndNomeIgnoreCaseAndTipo(usuarioId, nome, tipo)) {
            throw new RegraNegocioException("Já existe uma categoria de " + tipo.name().toLowerCase()
                    + " chamada \"" + nome + "\"");
        }
    }
}
