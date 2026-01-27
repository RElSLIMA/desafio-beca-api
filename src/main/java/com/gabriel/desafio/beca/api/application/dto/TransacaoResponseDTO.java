package com.gabriel.desafio.beca.api.application.dto;

import com.gabriel.desafio.beca.api.domain.model.StatusTransacao;
import com.gabriel.desafio.beca.api.domain.model.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransacaoResponseDTO(
        UUID id,
        BigDecimal valor,
        TipoTransacao tipo,
        StatusTransacao status,
        BigDecimal taxaCambio,
        LocalDateTime data
) {}