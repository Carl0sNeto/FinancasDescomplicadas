package com.carlos.financasdescomplicadas.exception;

public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso) {
        super(recurso + " não encontrado(a)");
    }
}
