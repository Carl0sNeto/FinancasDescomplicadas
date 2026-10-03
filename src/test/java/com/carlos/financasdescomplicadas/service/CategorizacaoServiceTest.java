package com.carlos.financasdescomplicadas.service;

import com.carlos.financasdescomplicadas.model.Categoria;
import com.carlos.financasdescomplicadas.model.RegraCategorizacao;
import com.carlos.financasdescomplicadas.model.TipoCategoria;
import com.carlos.financasdescomplicadas.model.Usuario;
import com.carlos.financasdescomplicadas.repository.RegraCategorizacaoRepository;
import com.carlos.financasdescomplicadas.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategorizacaoServiceTest {

    @Mock
    private RegraCategorizacaoRepository regraRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CategorizacaoService service;

    private final Usuario usuario = new Usuario("Teste", "teste@email.com", "hash");
    private final Categoria alimentacao = new Categoria(usuario, "Alimentação", TipoCategoria.DESPESA);
    private final Categoria transporte = new Categoria(usuario, "Transporte", TipoCategoria.DESPESA);
    private final Categoria salario = new Categoria(usuario, "Salário", TipoCategoria.RECEITA);

    @Test
    void normalizaRemovendoAcentosMaiusculasEEspacosExtras() {
        assertThat(CategorizacaoService.normalizar("  PADARIA   São João ")).isEqualTo("padaria sao joao");
        assertThat(CategorizacaoService.normalizar(null)).isEmpty();
    }

    @Test
    void sugereCategoriaQuandoPalavraChaveApareceNaDescricao() {
        var regras = List.of(regra(alimentacao, "ifood"), regra(transporte, "uber"));

        Optional<Categoria> sugestao = service.sugerirCategoria(
                "IFOOD *Restaurante Bom", new BigDecimal("-45.90"), regras);

        assertThat(sugestao).contains(alimentacao);
    }

    @Test
    void preferePalavraChaveMaisEspecifica() {
        // "uber" bateria com "Uber Eats", mas "uber eats" é mais específica
        var regras = List.of(regra(transporte, "uber"), regra(alimentacao, "uber eats"));

        Optional<Categoria> sugestao = service.sugerirCategoria(
                "Uber Eats Pedido 123", new BigDecimal("-30.00"), regras);

        assertThat(sugestao).contains(alimentacao);
    }

    @Test
    void ignoraRegraCujoTipoNaoCombinaComOSinalDoValor() {
        // Regra de despesa não deve categorizar uma receita (valor positivo)
        var regras = List.of(regra(alimentacao, "pix"));

        Optional<Categoria> sugestao = service.sugerirCategoria(
                "Pix recebido", new BigDecimal("100.00"), regras);

        assertThat(sugestao).isEmpty();
    }

    @Test
    void semRegraCorrespondenteNaoSugereNada() {
        var regras = List.of(regra(salario, "salario empresa"));

        assertThat(service.sugerirCategoria("Farmácia", new BigDecimal("-20"), regras)).isEmpty();
    }

    @Test
    void aprenderRegraSalvaPalavraChaveNormalizada() {
        UUID usuarioId = UUID.randomUUID();
        when(regraRepository.findByUsuarioIdAndPalavraChave(usuarioId, "padaria pao quente"))
                .thenReturn(Optional.empty());
        when(usuarioRepository.getReferenceById(usuarioId)).thenReturn(usuario);
        when(regraRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RegraCategorizacao regra = service.aprenderRegra(usuarioId, alimentacao, "Padaria Pão  Quente");

        assertThat(regra.getPalavraChave()).isEqualTo("padaria pao quente");
        assertThat(regra.getCategoria()).isEqualTo(alimentacao);
        verify(regraRepository).save(regra);
    }

    @Test
    void aprenderRegraExistenteAtualizaACategoria() {
        UUID usuarioId = UUID.randomUUID();
        RegraCategorizacao existente = regra(alimentacao, "uber");
        when(regraRepository.findByUsuarioIdAndPalavraChave(usuarioId, "uber"))
                .thenReturn(Optional.of(existente));
        when(regraRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RegraCategorizacao regra = service.aprenderRegra(usuarioId, transporte, "Uber");

        assertThat(regra).isSameAs(existente);
        assertThat(regra.getCategoria()).isEqualTo(transporte);
    }

    private RegraCategorizacao regra(Categoria categoria, String palavraChave) {
        return new RegraCategorizacao(usuario, categoria, palavraChave);
    }
}
