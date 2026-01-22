package com.gabriel.desafio.beca.api.domain.transacao;

import java.math.BigDecimal;
import java.util.List;

public record ExtratoDTO(
        String usuario,
        BigDecimal saldoAtual,
        List<Transacao> transacoes
) {}