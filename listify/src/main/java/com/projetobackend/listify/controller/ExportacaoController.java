package com.projetobackend.listify.controller;

import com.projetobackend.listify.service.ExportacaoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exportação de uma lista como texto (o PDF é gerado no navegador, pela impressão). */
@RestController
@RequestMapping("/listas/{listaId}/exportar")
public class ExportacaoController {

    private final ExportacaoService service;

    public ExportacaoController(ExportacaoService service) {
        this.service = service;
    }

    @GetMapping(value = "/texto", produces = "text/plain;charset=UTF-8")
    public String texto(@PathVariable Long listaId) {
        return service.texto(listaId);
    }
}
