package com.projetobackend.listify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ListaRequest(
        @NotBlank(message = "Informe o nome da lista")
        @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres") String nome) {
}
