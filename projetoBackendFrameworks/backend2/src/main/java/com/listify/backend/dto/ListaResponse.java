package com.listify.backend.dto;
import com.listify.backend.model.Lista;
import java.time.LocalDateTime;
import java.util.List;
public record ListaResponse(Long id, String nome, String status, LocalDateTime criadaEm, LocalDateTime concluidaEm, List<ItemResponse> itens) {
    public static ListaResponse de(Lista lista, List<ItemResponse> itens) {
        return new ListaResponse(lista.getId(), lista.getNome(), lista.getStatus(), lista.getCriadaEm(), lista.getConcluidaEm(), itens);
    }
}
