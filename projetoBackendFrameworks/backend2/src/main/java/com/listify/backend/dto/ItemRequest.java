package com.listify.backend.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ItemRequest(@NotBlank @Size(max=160) String nome, @Size(max=60) String quantidade, Boolean comprado) {}
