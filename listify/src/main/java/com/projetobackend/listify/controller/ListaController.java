package com.projetobackend.listify.controller;

import com.projetobackend.listify.dto.ListaRequest;
import com.projetobackend.listify.dto.ListaResponse;
import com.projetobackend.listify.model.HistoricoLista;
import com.projetobackend.listify.service.ListaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** Listas. Os itens ficam no ItemController (/listas/{id}/itens). */
@RestController
@RequestMapping("/listas")
public class ListaController {

    private final ListaService service;

    public ListaController(ListaService service) {
        this.service = service;
    }

    @GetMapping
    public List<ListaResponse> listar() {
        return service.listar();
    }

    @GetMapping("/historico")
    public List<HistoricoResponse> historico() {
        return service.historico().stream().map(HistoricoResponse::de).toList();
    }

    @GetMapping("/{id}")
    public ListaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListaResponse criar(@Valid @RequestBody ListaRequest request) {
        return service.criar(request);
    }

    @PutMapping("/{id}")
    public ListaResponse editar(@PathVariable Long id, @Valid @RequestBody ListaRequest request) {
        return service.editar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }

    @PostMapping("/{id}/duplicar")
    @ResponseStatus(HttpStatus.CREATED)
    public ListaResponse duplicar(@PathVariable Long id) {
        return service.duplicar(id);
    }

    @PostMapping("/{id}/concluir")
    public ListaResponse concluir(@PathVariable Long id) {
        return service.concluir(id);
    }

    public record HistoricoResponse(Long id, Long listaId, String nomeLista, LocalDateTime concluidaEm) {
        static HistoricoResponse de(HistoricoLista h) {
            return new HistoricoResponse(h.getId(), h.getListaId(), h.getNomeLista(), h.getConcluidaEm());
        }
    }
}
