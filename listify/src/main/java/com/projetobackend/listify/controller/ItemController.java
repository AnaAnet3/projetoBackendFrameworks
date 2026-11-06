package com.projetobackend.listify.controller;

import com.projetobackend.listify.dto.ItemDtos.*;
import com.projetobackend.listify.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ItemController {

    private final ItemService service;

    public ItemController(ItemService service) {
        this.service = service;
    }

    @PostMapping("/listas/{listaId}/itens")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse adicionar(@PathVariable Long listaId, @Valid @RequestBody ItemRequest req) {
        return service.adicionar(listaId, req);
    }

    @GetMapping("/listas/{listaId}/itens")
    public List<GrupoCategoria> listar(@PathVariable Long listaId) {
        return service.listar(listaId);
    }

    @PatchMapping("/listas/{listaId}/itens/{itemId}")
    public ItemResponse editar(@PathVariable Long listaId, @PathVariable Long itemId,
                               @Valid @RequestBody ItemUpdateRequest req) {
        return service.editar(listaId, itemId, req);
    }

    @DeleteMapping("/listas/{listaId}/itens/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long listaId, @PathVariable Long itemId) {
        service.remover(listaId, itemId);
    }

    @PatchMapping("/listas/{listaId}/itens/{itemId}/comprado")
    public ItemResponse marcarComprado(@PathVariable Long listaId, @PathVariable Long itemId,
                                       @Valid @RequestBody CompradoRequest req) {
        return service.marcarComprado(listaId, itemId, req);
    }

    @GetMapping("/listas/{listaId}/total")
    public TotalResponse total(@PathVariable Long listaId) {
        return service.total(listaId);
    }

    @GetMapping("/categorias")
    public List<CategoriaResponse> categorias() {
        return service.listarCategorias();
    }
}
