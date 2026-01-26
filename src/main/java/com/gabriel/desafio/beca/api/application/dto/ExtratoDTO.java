package com.gabriel.desafio.beca.api.application.dto;

import com.gabriel.desafio.beca.api.domain.model.Transacao;

import java.math.BigDecimal;
import java.util.List;

public record ExtratoDTO(
        String usuario,
        BigDecimal saldoAtual,
        List<Transacao> transacoes
) {}