package com.carlos.financasdescomplicadas.service;

import com.carlos.financasdescomplicadas.dto.MetaResponseDTO;
import com.carlos.financasdescomplicadas.dto.MovimentoMetaDTO;
import com.carlos.financasdescomplicadas.exception.RegraNegocioException;
import com.carlos.financasdescomplicadas.model.Meta;
import com.carlos.financasdescomplicadas.repository.MetaRepository;
import com.carlos.financasdescomplicadas.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetaServiceTest {

    @Mock
    private MetaRepository metaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private MetaService service;

    @Test
    void calculaPercentualEValorRestante() {
        MetaResponseDTO resposta = MetaService.paraResponse(meta("1000.00", "250.00"));

        assertThat(resposta.percentual()).isEqualByComparingTo("25.0");
        assertThat(resposta.valorRestante()).isEqualByComparingTo("750.00");
        assertThat(resposta.concluida()).isFalse();
    }

    @Test
    void metaUltrapassadaFicaEm100PorCentoESemValorRestante() {
        MetaResponseDTO resposta = MetaService.paraResponse(meta("500.00", "620.00"));

        assertThat(resposta.percentual()).isEqualByComparingTo("100");
        assertThat(resposta.valorRestante()).isEqualByComparingTo("0");
        assertThat(resposta.concluida()).isTrue();
    }

    @Test
    void movimentarSomaAoValorAtual() {
        UUID usuarioId = UUID.randomUUID();
        UUID metaId = UUID.randomUUID();
        when(metaRepository.findByIdAndUsuarioId(metaId, usuarioId)).thenReturn(Optional.of(meta("1000", "100")));

        MetaResponseDTO resposta = service.movimentar(usuarioId, metaId, new MovimentoMetaDTO(new BigDecimal("50")));

        assertThat(resposta.valorAtual()).isEqualByComparingTo("150");
    }

    @Test
    void naoPermiteRetirarMaisDoQueOGuardado() {
        UUID usuarioId = UUID.randomUUID();
        UUID metaId = UUID.randomUUID();
        when(metaRepository.findByIdAndUsuarioId(metaId, usuarioId)).thenReturn(Optional.of(meta("1000", "100")));

        assertThatThrownBy(() -> service.movimentar(usuarioId, metaId, new MovimentoMetaDTO(new BigDecimal("-150"))))
                .isInstanceOf(RegraNegocioException.class);
    }

    private Meta meta(String alvo, String atual) {
        Meta meta = new Meta();
        meta.setTitulo("Viagem");
        meta.setValorAlvo(new BigDecimal(alvo));
        meta.setValorAtual(new BigDecimal(atual));
        return meta;
    }
}
