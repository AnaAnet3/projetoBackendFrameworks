package com.projetobackend.listify.dto;

import com.projetobackend.listify.model.Lista;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Os itens da lista vêm pela rota /listas/{id}/itens (ItemController). */
public record ListaResponse(Long id, String nome, String status, LocalDateTime criadaEm,
                            LocalDateTime concluidaEm, BigDecimal limite, boolean modelo) {

    public static ListaResponse de(Lista l) {
        return new ListaResponse(l.getId(), l.getNome(), l.getStatus(), l.getCriadaEm(),
                l.getConcluidaEm(), l.getLimite(), l.isModelo());
    }
}
