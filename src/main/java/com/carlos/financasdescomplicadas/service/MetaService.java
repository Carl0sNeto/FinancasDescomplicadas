package com.carlos.financasdescomplicadas.service;

import com.carlos.financasdescomplicadas.dto.MetaRequestDTO;
import com.carlos.financasdescomplicadas.dto.MetaResponseDTO;
import com.carlos.financasdescomplicadas.dto.MovimentoMetaDTO;
import com.carlos.financasdescomplicadas.exception.RecursoNaoEncontradoException;
import com.carlos.financasdescomplicadas.exception.RegraNegocioException;
import com.carlos.financasdescomplicadas.model.Meta;
import com.carlos.financasdescomplicadas.repository.MetaRepository;
import com.carlos.financasdescomplicadas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class MetaService {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final MetaRepository metaRepository;
    private final UsuarioRepository usuarioRepository;

    public MetaService(MetaRepository metaRepository, UsuarioRepository usuarioRepository) {
        this.metaRepository = metaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<MetaResponseDTO> listar(UUID usuarioId) {
        return metaRepository.findByUsuarioIdOrderByDataLimiteAsc(usuarioId).stream()
                .map(MetaService::paraResponse)
                .toList();
    }

    @Transactional
    public MetaResponseDTO criar(UUID usuarioId, MetaRequestDTO dto) {
        Meta meta = new Meta();
        meta.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        preencher(meta, dto);
        return paraResponse(metaRepository.save(meta));
    }

    @Transactional
    public MetaResponseDTO atualizar(UUID usuarioId, UUID metaId, MetaRequestDTO dto) {
        Meta meta = buscarDoUsuario(usuarioId, metaId);
        preencher(meta, dto);
        return paraResponse(meta);
    }

    // Guarda (valor positivo) ou retira (valor negativo) dinheiro da meta
    @Transactional
    public MetaResponseDTO movimentar(UUID usuarioId, UUID metaId, MovimentoMetaDTO dto) {
        if (dto.valor().signum() == 0) {
            throw new RegraNegocioException("O valor não pode ser zero");
        }
        Meta meta = buscarDoUsuario(usuarioId, metaId);
        BigDecimal novoValor = meta.getValorAtual().add(dto.valor());
        if (novoValor.signum() < 0) {
            throw new RegraNegocioException("Não dá pra retirar mais do que já foi guardado na meta");
        }
        meta.setValorAtual(novoValor);
        return paraResponse(meta);
    }

    @Transactional
    public void excluir(UUID usuarioId, UUID metaId) {
        metaRepository.delete(buscarDoUsuario(usuarioId, metaId));
    }

    static MetaResponseDTO paraResponse(Meta meta) {
        BigDecimal alvo = meta.getValorAlvo();
        BigDecimal atual = meta.getValorAtual();

        BigDecimal percentual = atual.multiply(CEM)
                .divide(alvo, 1, RoundingMode.DOWN)
                .min(CEM);
        BigDecimal restante = alvo.subtract(atual).max(BigDecimal.ZERO);

        return new MetaResponseDTO(
                meta.getId(),
                meta.getTitulo(),
                alvo,
                atual,
                meta.getDataLimite(),
                percentual,
                restante,
                atual.compareTo(alvo) >= 0
        );
    }

    private void preencher(Meta meta, MetaRequestDTO dto) {
        meta.setTitulo(dto.titulo().trim());
        meta.setValorAlvo(dto.valorAlvo());
        meta.setValorAtual(dto.valorAtual() != null ? dto.valorAtual() : BigDecimal.ZERO);
        meta.setDataLimite(dto.dataLimite());
    }

    private Meta buscarDoUsuario(UUID usuarioId, UUID metaId) {
        return metaRepository.findByIdAndUsuarioId(metaId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Meta"));
    }
}
