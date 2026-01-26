package com.gabriel.desafio.beca.api.application.dto;

import java.math.BigDecimal;

public record SaldoExternoDTO(
        String id,
        String usuarioId,
        BigDecimal saldo
) {}