package com.gabriel.desafio.beca.api.domain.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.br.CPF;

public record UsuarioDTO(
        @NotBlank(message = "O nome não pode estar vazio")
        String nome,

        @NotBlank(message = "O email é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        String senha,

        @NotBlank(message = "O CPF é obrigatório")
        @CPF(message = "CPF inválido")
        String cpf
) {}