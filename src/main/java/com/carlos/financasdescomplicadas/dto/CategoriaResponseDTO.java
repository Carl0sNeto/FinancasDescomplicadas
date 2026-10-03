package com.carlos.financasdescomplicadas.dto;

import com.carlos.financasdescomplicadas.model.Categoria;
import com.carlos.financasdescomplicadas.model.TipoCategoria;

import java.util.UUID;

public record CategoriaResponseDTO(UUID id, String nome, TipoCategoria tipo) {

    public static CategoriaResponseDTO de(Categoria categoria) {
        return new CategoriaResponseDTO(categoria.getId(), categoria.getNome(), categoria.getTipo());
    }
}
