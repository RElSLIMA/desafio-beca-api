package com.gabriel.desafio.beca.api.domain.transacao;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record TransacaoDTO(
        @NotNull(message = "O valor é obrigatório")
        @Positive(message = "O valor deve ser maior que zero")
        BigDecimal valor,

        @NotNull(message = "O tipo da transação é obrigatório (DEPOSITO, SAQUE, TRANSFERENCIA, COMPRA)")
        TipoTransacao tipo,

        @NotNull(message = "O ID do usuário é obrigatório")
        UUID usuarioId
) {}