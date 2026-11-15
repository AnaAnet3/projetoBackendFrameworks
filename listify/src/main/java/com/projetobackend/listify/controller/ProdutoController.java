package com.projetobackend.listify.controller;

import com.projetobackend.listify.dto.ProdutoResponse;
import com.projetobackend.listify.service.ProdutoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService service;

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProdutoResponse> buscar(@RequestParam(defaultValue = "") String busca) {
        return service.buscar(busca);
    }
}
