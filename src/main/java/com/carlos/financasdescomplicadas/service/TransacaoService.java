package com.carlos.financasdescomplicadas.service;

import com.carlos.financasdescomplicadas.dto.CategorizarTransacaoDTO;
import com.carlos.financasdescomplicadas.dto.ResumoDTO;
import com.carlos.financasdescomplicadas.dto.TotaisPeriodoDTO;
import com.carlos.financasdescomplicadas.dto.TransacaoRequestDTO;
import com.carlos.financasdescomplicadas.dto.TransacaoResponseDTO;
import com.carlos.financasdescomplicadas.exception.RecursoNaoEncontradoException;
import com.carlos.financasdescomplicadas.exception.RegraNegocioException;
import com.carlos.financasdescomplicadas.model.Categoria;
import com.carlos.financasdescomplicadas.model.RegraCategorizacao;
import com.carlos.financasdescomplicadas.model.TipoCategoria;
import com.carlos.financasdescomplicadas.model.Transacao;
import com.carlos.financasdescomplicadas.repository.TransacaoRepository;
import com.carlos.financasdescomplicadas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaService categoriaService;
    private final CategorizacaoService categorizacaoService;

    public TransacaoService(TransacaoRepository transacaoRepository,
                            UsuarioRepository usuarioRepository,
                            CategoriaService categoriaService,
                            CategorizacaoService categorizacaoService) {
        this.transacaoRepository = transacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaService = categoriaService;
        this.categorizacaoService = categorizacaoService;
    }

    /**
     * Lista as transações do período (padrão: mês atual). Com {@code pendentes = true}
     * devolve todas as transações sem categoria, de qualquer data.
     */
    @Transactional(readOnly = true)
    public List<TransacaoResponseDTO> listar(UUID usuarioId, LocalDate inicio, LocalDate fim,
                                             UUID categoriaId, boolean pendentes) {
        List<Transacao> transacoes;
        if (pendentes) {
            transacoes = transacaoRepository.findByUsuarioIdAndCategoriaIsNullOrderByDataDesc(usuarioId);
        } else {
            Periodo periodo = Periodo.de(inicio, fim);
            transacoes = categoriaId == null
                    ? transacaoRepository.findByUsuarioIdAndDataBetweenOrderByDataDescCriadoEmDesc(
                            usuarioId, periodo.inicio(), periodo.fim())
                    : transacaoRepository.findByUsuarioIdAndCategoriaIdAndDataBetweenOrderByDataDescCriadoEmDesc(
                            usuarioId, categoriaId, periodo.inicio(), periodo.fim());
        }
        return transacoes.stream().map(TransacaoResponseDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public TransacaoResponseDTO buscar(UUID usuarioId, UUID transacaoId) {
        return TransacaoResponseDTO.de(buscarDoUsuario(usuarioId, transacaoId));
    }

    @Transactional
    public TransacaoResponseDTO criar(UUID usuarioId, TransacaoRequestDTO dto) {
        Transacao transacao = new Transacao();
        transacao.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        preencher(usuarioId, transacao, dto);
        return TransacaoResponseDTO.de(transacaoRepository.save(transacao));
    }

    @Transactional
    public TransacaoResponseDTO atualizar(UUID usuarioId, UUID transacaoId, TransacaoRequestDTO dto) {
        Transacao transacao = buscarDoUsuario(usuarioId, transacaoId);
        preencher(usuarioId, transacao, dto);
        return TransacaoResponseDTO.de(transacao);
    }

    /**
     * Correção manual de categoria. Por padrão vira uma regra de categorização, pra que a
     * próxima importação já acerte sozinha — e a regra nova também é aplicada às outras
     * transações que ainda estão pendentes.
     */
    @Transactional
    public TransacaoResponseDTO categorizar(UUID usuarioId, UUID transacaoId, CategorizarTransacaoDTO dto) {
        Transacao transacao = buscarDoUsuario(usuarioId, transacaoId);
        Categoria categoria = categoriaService.buscarDoUsuario(usuarioId, dto.categoriaId());
        validarTipo(categoria, transacao.getValor());
        transacao.setCategoria(categoria);

        if (dto.deveLembrar()) {
            String palavraChave = dto.palavraChave() == null || dto.palavraChave().isBlank()
                    ? transacao.getDescricao()
                    : dto.palavraChave();
            RegraCategorizacao regra = categorizacaoService.aprenderRegra(usuarioId, categoria, palavraChave);
            aplicarNasPendentes(usuarioId, regra);
        }
        return TransacaoResponseDTO.de(transacao);
    }

    private void aplicarNasPendentes(UUID usuarioId, RegraCategorizacao regra) {
        List<RegraCategorizacao> regras = List.of(regra);
        for (Transacao pendente : transacaoRepository.findByUsuarioIdAndCategoriaIsNullOrderByDataDesc(usuarioId)) {
            categorizacaoService.sugerirCategoria(pendente.getDescricao(), pendente.getValor(), regras)
                    .ifPresent(pendente::setCategoria);
        }
    }

    @Transactional
    public void excluir(UUID usuarioId, UUID transacaoId) {
        transacaoRepository.delete(buscarDoUsuario(usuarioId, transacaoId));
    }

    @Transactional(readOnly = true)
    public ResumoDTO resumo(UUID usuarioId, LocalDate inicio, LocalDate fim) {
        Periodo periodo = Periodo.de(inicio, fim);
        TotaisPeriodoDTO totais = transacaoRepository.totaisDoPeriodo(usuarioId, periodo.inicio(), periodo.fim());

        return new ResumoDTO(
                periodo.inicio(),
                periodo.fim(),
                totais.receitas(),
                totais.despesas(),
                totais.receitas().subtract(totais.despesas()),
                transacaoRepository.countByUsuarioIdAndCategoriaIsNull(usuarioId),
                transacaoRepository.despesasPorCategoria(usuarioId, periodo.inicio(), periodo.fim())
        );
    }

    private void preencher(UUID usuarioId, Transacao transacao, TransacaoRequestDTO dto) {
        if (dto.valor().signum() == 0) {
            throw new RegraNegocioException("O valor não pode ser zero");
        }

        Categoria categoria = null;
        if (dto.categoriaId() != null) {
            categoria = categoriaService.buscarDoUsuario(usuarioId, dto.categoriaId());
            validarTipo(categoria, dto.valor());
        }

        transacao.setDescricao(dto.descricao().trim());
        transacao.setValor(dto.valor());
        transacao.setData(dto.data());
        transacao.setCategoria(categoria);
    }

    // Evita, por exemplo, uma despesa (valor negativo) numa categoria de receita
    private void validarTipo(Categoria categoria, BigDecimal valor) {
        TipoCategoria tipoDoValor = TipoCategoria.doValor(valor);
        if (categoria.getTipo() != tipoDoValor) {
            throw new RegraNegocioException("A categoria \"" + categoria.getNome() + "\" é de "
                    + categoria.getTipo().name().toLowerCase() + ", mas o valor indica uma "
                    + tipoDoValor.name().toLowerCase());
        }
    }

    private Transacao buscarDoUsuario(UUID usuarioId, UUID transacaoId) {
        return transacaoRepository.findByIdAndUsuarioId(transacaoId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Transação"));
    }

    private record Periodo(LocalDate inicio, LocalDate fim) {

        // Sem datas informadas, usa o mês atual
        static Periodo de(LocalDate inicio, LocalDate fim) {
            YearMonth mesAtual = YearMonth.now();
            LocalDate i = inicio != null ? inicio : mesAtual.atDay(1);
            LocalDate f = fim != null ? fim : mesAtual.atEndOfMonth();
            if (f.isBefore(i)) {
                throw new RegraNegocioException("A data final não pode ser anterior à inicial");
            }
            return new Periodo(i, f);
        }
    }
}
