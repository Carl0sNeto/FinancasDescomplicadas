package com.carlos.financasdescomplicadas.dto;

import com.carlos.financasdescomplicadas.model.Categoria;
import com.carlos.financasdescomplicadas.model.TipoCategoria;
import com.carlos.financasdescomplicadas.model.Transacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransacaoResponseDTO(
        UUID id,
        String descricao,
        BigDecimal valor,
        LocalDate data,
        TipoCategoria tipo,
        UUID categoriaId,
        String categoriaNome,
        UUID importacaoId,
        boolean pendente
) {
    public static TransacaoResponseDTO de(Transacao transacao) {
        Categoria categoria = transacao.getCategoria();
        UUID importacaoId = transacao.getImportacao() != null ? transacao.getImportacao().getId() : null;
        return new TransacaoResponseDTO(
                transacao.getId(),
                transacao.getDescricao(),
                transacao.getValor(),
                transacao.getData(),
                transacao.getTipo(),
                categoria != null ? categoria.getId() : null,
                categoria != null ? categoria.getNome() : null,
                importacaoId,
                categoria == null
        );
    }
}
