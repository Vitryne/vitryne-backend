package com.vitryne.api.exception;

public class QuantidadeInvalidaException extends RuntimeException {
    public QuantidadeInvalidaException(Integer quantidade) {
        super(String.format("Quantidade inválida: (%d)", quantidade));
    }
}
