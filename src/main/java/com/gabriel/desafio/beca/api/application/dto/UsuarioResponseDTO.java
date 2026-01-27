package com.gabriel.desafio.beca.api.application.dto;

import java.util.UUID;

public record UsuarioResponseDTO(
        UUID id,
        String nome,
        String email,
        String cpf
) {}