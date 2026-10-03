package com.carlos.financasdescomplicadas.service;

import com.carlos.financasdescomplicadas.dto.NovaSenhaDTO;
import com.carlos.financasdescomplicadas.dto.NovoUsuarioDTO;
import com.carlos.financasdescomplicadas.dto.UsuarioResponseDTO;
import com.carlos.financasdescomplicadas.exception.RecursoNaoEncontradoException;
import com.carlos.financasdescomplicadas.exception.RegraNegocioException;
import com.carlos.financasdescomplicadas.model.Papel;
import com.carlos.financasdescomplicadas.model.Usuario;
import com.carlos.financasdescomplicadas.repository.CategoriaRepository;
import com.carlos.financasdescomplicadas.repository.ImportacaoRepository;
import com.carlos.financasdescomplicadas.repository.MetaRepository;
import com.carlos.financasdescomplicadas.repository.RegraCategorizacaoRepository;
import com.carlos.financasdescomplicadas.repository.TransacaoRepository;
import com.carlos.financasdescomplicadas.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Gestão de contas pela página de administração. Não existe cadastro público:
 * só administradores criam contas (ver docs/05-seguranca-autenticacao.md).
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CategoriaService categoriaService;
    private final PasswordEncoder passwordEncoder;
    private final TransacaoRepository transacaoRepository;
    private final RegraCategorizacaoRepository regraRepository;
    private final CategoriaRepository categoriaRepository;
    private final MetaRepository metaRepository;
    private final ImportacaoRepository importacaoRepository;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          CategoriaService categoriaService,
                          PasswordEncoder passwordEncoder,
                          TransacaoRepository transacaoRepository,
                          RegraCategorizacaoRepository regraRepository,
                          CategoriaRepository categoriaRepository,
                          MetaRepository metaRepository,
                          ImportacaoRepository importacaoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.categoriaService = categoriaService;
        this.passwordEncoder = passwordEncoder;
        this.transacaoRepository = transacaoRepository;
        this.regraRepository = regraRepository;
        this.categoriaRepository = categoriaRepository;
        this.metaRepository = metaRepository;
        this.importacaoRepository = importacaoRepository;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listar() {
        return usuarioRepository.findAllByOrderByNomeAsc().stream()
                .map(UsuarioResponseDTO::de)
                .toList();
    }

    // A conta nova já vem com as categorias padrão, como a conta inicial
    @Transactional
    public UsuarioResponseDTO criar(NovoUsuarioDTO dto) {
        String email = dto.email().trim();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new RegraNegocioException("Já existe uma conta com o email " + email);
        }

        Usuario usuario = new Usuario(dto.nome().trim(), email, passwordEncoder.encode(dto.senha()));
        usuario.setPapel(dto.papel() != null ? dto.papel() : Papel.USUARIO);
        usuarioRepository.save(usuario);
        categoriaService.criarCategoriasPadrao(usuario);
        return UsuarioResponseDTO.de(usuario);
    }

    @Transactional
    public void trocarSenha(UUID usuarioId, NovaSenhaDTO dto) {
        buscar(usuarioId).setSenhaHash(passwordEncoder.encode(dto.senha()));
    }

    /**
     * Exclui a conta e todos os dados dela. A ordem respeita as chaves estrangeiras:
     * transações apontam pra importações e categorias, e regras apontam pra categorias.
     */
    @Transactional
    public void excluir(UUID adminId, UUID usuarioId) {
        if (adminId.equals(usuarioId)) {
            throw new RegraNegocioException("Você não pode excluir a própria conta");
        }
        Usuario usuario = buscar(usuarioId);

        transacaoRepository.deleteByUsuarioId(usuarioId);
        regraRepository.deleteByUsuarioId(usuarioId);
        categoriaRepository.deleteByUsuarioId(usuarioId);
        metaRepository.deleteByUsuarioId(usuarioId);
        importacaoRepository.deleteByUsuarioId(usuarioId);
        usuarioRepository.delete(usuario);
    }

    private Usuario buscar(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário"));
    }
}
