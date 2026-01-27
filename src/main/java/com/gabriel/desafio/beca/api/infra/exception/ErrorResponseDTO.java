package com.gabriel.desafio.beca.api.infra.exception;

import java.util.List;

public record ErrorResponseDTO(
        String mensagem,
        List<ValidationError> errosValidacao
) {
    public ErrorResponseDTO(String mensagem) {
        this(mensagem, null);
    }

    public record ValidationError(String campo, String erro) {}
}