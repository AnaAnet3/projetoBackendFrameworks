package com.projetobackend.listify.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** limite é opcional: sem ele (ou null), a lista fica sem orçamento. */
public record ListaRequest(
        @NotBlank(message = "Informe o nome da lista")
        @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres") String nome,
        @Positive(message = "O limite deve ser maior que zero")
        @Digits(integer = 8, fraction = 2, message = "Limite inválido") BigDecimal limite) {
}
