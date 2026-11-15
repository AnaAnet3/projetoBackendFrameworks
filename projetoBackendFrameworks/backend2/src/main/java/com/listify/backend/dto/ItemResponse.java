package com.listify.backend.dto;
import com.listify.backend.model.Item;
public record ItemResponse(Long id, String nome, String quantidade, boolean comprado) {
    public static ItemResponse de(Item item) { return new ItemResponse(item.getId(), item.getNome(), item.getQuantidade(), item.isComprado()); }
}
