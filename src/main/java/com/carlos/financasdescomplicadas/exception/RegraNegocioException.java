package com.carlos.financasdescomplicadas.exception;

// Violação de regra de negócio ou entrada inválida que a validação do DTO não cobre
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }

    public RegraNegocioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
