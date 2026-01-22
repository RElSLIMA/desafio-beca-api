package com.gabriel.desafio.beca.api.infra.client;

import java.math.BigDecimal;

public record SaldoExternoDTO(
        String id,
        BigDecimal saldo
) {}