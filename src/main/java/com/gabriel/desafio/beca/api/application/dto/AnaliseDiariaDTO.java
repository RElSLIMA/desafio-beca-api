package com.gabriel.desafio.beca.api.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AnaliseDiariaDTO(
        LocalDate data,
        BigDecimal total
) {}