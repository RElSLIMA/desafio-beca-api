package com.gabriel.desafio.beca.api.application.dto;

import com.gabriel.desafio.beca.api.domain.model.CategoriaTransacao;
import java.math.BigDecimal;

public record AnaliseCategoriaDTO(
        CategoriaTransacao categoria,
        BigDecimal total
) {}