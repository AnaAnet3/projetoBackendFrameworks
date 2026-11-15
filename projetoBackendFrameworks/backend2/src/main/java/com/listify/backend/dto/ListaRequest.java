package com.listify.backend.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ListaRequest(@NotBlank @Size(max=120) String nome) {}
