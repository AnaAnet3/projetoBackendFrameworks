package com.projetobackend.listify.dto;

import com.projetobackend.listify.model.Lista;

import java.time.LocalDateTime;

/** Os itens da lista vêm pela rota /listas/{id}/itens (ItemController). */
public record ListaResponse(Long id, String nome, String status, LocalDateTime criadaEm, LocalDateTime concluidaEm) {

    public static ListaResponse de(Lista l) {
        return new ListaResponse(l.getId(), l.getNome(), l.getStatus(), l.getCriadaEm(), l.getConcluidaEm());
    }
}
