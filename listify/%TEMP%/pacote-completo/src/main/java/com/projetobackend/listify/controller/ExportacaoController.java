package com.projetobackend.listify.controller;

import com.projetobackend.listify.service.ExportacaoService;
import com.projetobackend.listify.service.ExportacaoService.Arquivo;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/** Exportação de uma lista: CSV (download) e texto para WhatsApp. */
@RestController
@RequestMapping("/listas/{listaId}/exportar")
public class ExportacaoController {

    private final ExportacaoService service;

    public ExportacaoController(ExportacaoService service) {
        this.service = service;
    }

    @GetMapping("/csv")
    public ResponseEntity<byte[]> csv(@PathVariable Long listaId) {
        Arquivo arquivo = service.csv(listaId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(arquivo.nome()).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(arquivo.conteudo());
    }

    @GetMapping(value = "/texto", produces = "text/plain;charset=UTF-8")
    public String texto(@PathVariable Long listaId) {
        return service.texto(listaId);
    }
}
