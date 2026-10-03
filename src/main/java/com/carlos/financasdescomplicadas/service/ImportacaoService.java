package com.carlos.financasdescomplicadas.service;

import com.carlos.financasdescomplicadas.dto.ImportacaoResponseDTO;
import com.carlos.financasdescomplicadas.dto.ResultadoImportacaoDTO;
import com.carlos.financasdescomplicadas.dto.TransacaoBruta;
import com.carlos.financasdescomplicadas.exception.RecursoNaoEncontradoException;
import com.carlos.financasdescomplicadas.exception.RegraNegocioException;
import com.carlos.financasdescomplicadas.model.Importacao;
import com.carlos.financasdescomplicadas.model.RegraCategorizacao;
import com.carlos.financasdescomplicadas.model.Transacao;
import com.carlos.financasdescomplicadas.model.Usuario;
import com.carlos.financasdescomplicadas.parser.ExtratoParser;
import com.carlos.financasdescomplicadas.repository.ImportacaoRepository;
import com.carlos.financasdescomplicadas.repository.TransacaoRepository;
import com.carlos.financasdescomplicadas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Importação de extrato: escolhe o parser pelo nome do arquivo, cria as transações e
 * tenta categorizar cada uma pelas regras do usuário (ver docs/04-importacao-automatica.md).
 */
@Service
public class ImportacaoService {

    private static final int TAMANHO_MAXIMO_DESCRICAO = 255;

    private final List<ExtratoParser> parsers;
    private final CategorizacaoService categorizacaoService;
    private final ImportacaoRepository importacaoRepository;
    private final TransacaoRepository transacaoRepository;
    private final UsuarioRepository usuarioRepository;

    public ImportacaoService(List<ExtratoParser> parsers,
                             CategorizacaoService categorizacaoService,
                             ImportacaoRepository importacaoRepository,
                             TransacaoRepository transacaoRepository,
                             UsuarioRepository usuarioRepository) {
        this.parsers = parsers;
        this.categorizacaoService = categorizacaoService;
        this.importacaoRepository = importacaoRepository;
        this.transacaoRepository = transacaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public ResultadoImportacaoDTO importar(UUID usuarioId, String nomeArquivo, InputStream conteudo) {
        ExtratoParser parser = parsers.stream()
                .filter(p -> p.suporta(nomeArquivo))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException(
                        "Formato de arquivo não suportado. Envie um extrato .ofx ou .csv"));

        List<TransacaoBruta> brutas = parser.parse(conteudo);
        if (brutas.isEmpty()) {
            throw new RegraNegocioException("Nenhuma transação encontrada no arquivo");
        }

        Usuario usuario = usuarioRepository.getReferenceById(usuarioId);
        Importacao importacao = new Importacao();
        importacao.setUsuario(usuario);
        importacao.setNomeArquivo(nomeArquivo);
        importacao.setFormato(parser.formato());
        importacao.setTotalTransacoes(brutas.size());
        importacaoRepository.save(importacao);

        List<RegraCategorizacao> regras = categorizacaoService.carregarRegras(usuarioId);
        List<Transacao> transacoes = new ArrayList<>();
        int categorizadas = 0;

        for (TransacaoBruta bruta : brutas) {
            if (bruta.valor().signum() == 0) {
                continue;
            }
            Transacao transacao = new Transacao();
            transacao.setUsuario(usuario);
            transacao.setImportacao(importacao);
            transacao.setDescricao(limitar(bruta.descricao()));
            transacao.setValor(bruta.valor());
            transacao.setData(bruta.data());

            // Sem regra correspondente a transação fica pendente de revisão manual
            var categoria = categorizacaoService.sugerirCategoria(bruta.descricao(), bruta.valor(), regras);
            if (categoria.isPresent()) {
                transacao.setCategoria(categoria.get());
                categorizadas++;
            }
            transacoes.add(transacao);
        }

        transacaoRepository.saveAll(transacoes);
        importacao.setTotalTransacoes(transacoes.size());
        // Força o INSERT agora pra que importadoEm (@CreationTimestamp) já venha preenchido na resposta
        importacaoRepository.flush();

        return new ResultadoImportacaoDTO(
                ImportacaoResponseDTO.de(importacao),
                categorizadas,
                transacoes.size() - categorizadas);
    }

    @Transactional(readOnly = true)
    public List<ImportacaoResponseDTO> listar(UUID usuarioId) {
        return importacaoRepository.findByUsuarioIdOrderByImportadoEmDesc(usuarioId).stream()
                .map(ImportacaoResponseDTO::de)
                .toList();
    }

    // Desfaz uma importação: remove ela e todas as transações que ela criou
    @Transactional
    public void desfazer(UUID usuarioId, UUID importacaoId) {
        Importacao importacao = importacaoRepository.findByIdAndUsuarioId(importacaoId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Importação"));
        transacaoRepository.deleteByImportacaoId(importacaoId);
        importacaoRepository.delete(importacao);
    }

    private static String limitar(String descricao) {
        return descricao.length() <= TAMANHO_MAXIMO_DESCRICAO
                ? descricao
                : descricao.substring(0, TAMANHO_MAXIMO_DESCRICAO);
    }
}
