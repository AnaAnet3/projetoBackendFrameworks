package com.listify.backend.controller;

import com.listify.backend.dto.*;
import com.listify.backend.model.HistoricoLista;
import com.listify.backend.service.ListaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/listas")
@CrossOrigin(origins = "*")
public class ListaController {
    private final ListaService service;
    public ListaController(ListaService service) { this.service = service; }

    @GetMapping public List<ListaResponse> listar() { return service.listar(); }
    @GetMapping("/{id}") public ListaResponse buscar(@PathVariable Long id) { return service.buscar(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ListaResponse criar(@Valid @RequestBody ListaRequest request) { return service.criar(request); }
    @PutMapping("/{id}") public ListaResponse editar(@PathVariable Long id, @Valid @RequestBody ListaRequest request) { return service.editar(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) { service.excluir(id); }
    @PostMapping("/{id}/duplicar") @ResponseStatus(HttpStatus.CREATED)
    public ListaResponse duplicar(@PathVariable Long id) { return service.duplicar(id); }
    @PostMapping("/{id}/concluir") public ListaResponse concluir(@PathVariable Long id) { return service.concluir(id); }

    @GetMapping("/historico")
    public List<HistoricoResponse> historico() {
        return service.historico().stream().map(HistoricoResponse::de).toList();
    }

    @PostMapping("/{listaId}/itens") @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse adicionarItem(@PathVariable Long listaId, @Valid @RequestBody ItemRequest request) { return service.adicionarItem(listaId, request); }
    @PutMapping("/{listaId}/itens/{itemId}")
    public ItemResponse editarItem(@PathVariable Long listaId, @PathVariable Long itemId, @Valid @RequestBody ItemRequest request) { return service.editarItem(listaId, itemId, request); }
    @DeleteMapping("/{listaId}/itens/{itemId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirItem(@PathVariable Long listaId, @PathVariable Long itemId) { service.excluirItem(listaId, itemId); }

    public record HistoricoResponse(Long id, Long listaId, String nomeLista, LocalDateTime concluidaEm) {
        static HistoricoResponse de(HistoricoLista h) { return new HistoricoResponse(h.getId(), h.getListaId(), h.getNomeLista(), h.getConcluidaEm()); }
    }
}
