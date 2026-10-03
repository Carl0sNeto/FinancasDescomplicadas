package com.carlos.financasdescomplicadas.parser;

import com.carlos.financasdescomplicadas.dto.TransacaoBruta;
import com.carlos.financasdescomplicadas.model.FormatoExtrato;

import java.io.InputStream;
import java.util.List;

/**
 * Strategy de leitura de extrato bancário. Cada implementação sabe ler um formato;
 * o ImportacaoService escolhe a certa pelo nome do arquivo.
 */
public interface ExtratoParser {

    boolean suporta(String nomeArquivo);

    List<TransacaoBruta> parse(InputStream arquivo);

    /**
     * Versão com a senha do arquivo, usada por formatos que podem vir protegidos (PDF).
     * Os demais formatos ignoram a senha.
     */
    default List<TransacaoBruta> parse(InputStream arquivo, String senha) {
        return parse(arquivo);
    }

    FormatoExtrato formato();
}
