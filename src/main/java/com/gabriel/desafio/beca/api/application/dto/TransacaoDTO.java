package com.gabriel.desafio.beca.api.application.dto;

import com.gabriel.desafio.beca.api.domain.model.CategoriaTransacao;
import com.gabriel.desafio.beca.api.domain.model.TipoTransacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record TransacaoDTO(
        @NotNull(message = "Valor obrigatório")
        @Positive(message = "Valor deve ser positivo")
        BigDecimal valor,

        @NotNull(message = "Tipo da transação obrigatório")
        TipoTransacao tipo,

        @NotNull(message = "ID do usuário obrigatório")
        UUID usuarioId,

        CategoriaTransacao categoria,

        UUID destinatarioId,

        String moeda
) {
    public TransacaoDTO {
        if (moeda == null) {
            moeda = "BRL";
        }
        if (categoria == null) {
            categoria = CategoriaTransacao.OUTROS;
        }
    }
}