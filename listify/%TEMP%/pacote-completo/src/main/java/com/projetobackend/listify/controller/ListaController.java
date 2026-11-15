package com.projetobackend.listify.controller;

import com.projetobackend.listify.dto.HistoricoResponse;
import com.projetobackend.listify.dto.ListaRequest;
import com.projetobackend.listify.dto.ListaResponse;
import com.projetobackend.listify.service.ListaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
        return service.historico();
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

    /** Guarda a lista como modelo reutilizável (ex.: "Compra do mês"). */
    @PostMapping("/{id}/salvar-modelo")
    @ResponseStatus(HttpStatus.CREATED)
    public ListaResponse salvarComoModelo(@PathVariable Long id) {
        return service.salvarComoModelo(id);
    }

    /** Cria uma lista nova e ativa a partir de um modelo. */
    @PostMapping("/{id}/usar-modelo")
    @ResponseStatus(HttpStatus.CREATED)
    public ListaResponse usarModelo(@PathVariable Long id) {
        return service.usarModelo(id);
    }

    @PostMapping("/{id}/concluir")
    public ListaResponse concluir(@PathVariable Long id) {
        return service.concluir(id);
    }

}
