package com.listify.backend.service;

import com.listify.backend.dto.*;
import com.listify.backend.model.*;
import com.listify.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ListaService {
    private final ListaRepository listas;
    private final ItemRepository itens;
    private final HistoricoListaRepository historico;

    public ListaService(ListaRepository listas, ItemRepository itens, HistoricoListaRepository historico) {
        this.listas = listas; this.itens = itens; this.historico = historico;
    }

    @Transactional(readOnly = true)
    public List<ListaResponse> listar() {
        return listas.findAll().stream().map(this::resposta).toList();
    }

    @Transactional(readOnly = true)
    public ListaResponse buscar(Long id) { return resposta(obterLista(id)); }

    @Transactional
    public ListaResponse criar(ListaRequest request) {
        Lista lista = new Lista(); lista.setNome(request.nome().trim());
        return resposta(listas.save(lista));
    }

    @Transactional
    public ListaResponse editar(Long id, ListaRequest request) {
        Lista lista = obterLista(id);
        if ("CONCLUIDA".equals(lista.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível editar uma lista concluída.");
        lista.setNome(request.nome().trim());
        return resposta(listas.save(lista));
    }

    @Transactional
    public void excluir(Long id) { listas.delete(obterLista(id)); }

    @Transactional
    public ListaResponse duplicar(Long id) {
        Lista original = obterLista(id);
        Lista copia = new Lista(); copia.setNome(original.getNome() + " (cópia)");
        copia = listas.save(copia);
        for (Item item : itens.findByLista_IdOrderByIdAsc(id)) {
            Item novo = new Item(); novo.setLista(copia); novo.setNome(item.getNome());
            novo.setQuantidade(item.getQuantidade()); novo.setComprado(false); itens.save(novo);
        }
        return resposta(copia);
    }

    @Transactional
    public ListaResponse concluir(Long id) {
        Lista lista = obterLista(id);
        if (!"CONCLUIDA".equals(lista.getStatus())) {
            LocalDateTime agora = LocalDateTime.now();
            lista.setStatus("CONCLUIDA"); lista.setConcluidaEm(agora);
            listas.save(lista);
            historico.save(new HistoricoLista(lista, lista.getNome(), agora));
        }
        return resposta(lista);
    }

    @Transactional(readOnly = true)
    public List<HistoricoLista> historico() { return historico.findAllByOrderByConcluidaEmDesc(); }

    @Transactional
    public ItemResponse adicionarItem(Long listaId, ItemRequest request) {
        Lista lista = obterLista(listaId); validarAberta(lista);
        Item item = new Item(); item.setLista(lista); item.setNome(request.nome().trim());
        item.setQuantidade(request.quantidade()); item.setComprado(Boolean.TRUE.equals(request.comprado()));
        return ItemResponse.de(itens.save(item));
    }

    @Transactional
    public ItemResponse editarItem(Long listaId, Long itemId, ItemRequest request) {
        Lista lista = obterLista(listaId); validarAberta(lista);
        Item item = itens.findById(itemId).filter(i -> i.getLista().getId().equals(listaId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado."));
        item.setNome(request.nome().trim()); item.setQuantidade(request.quantidade());
        if (request.comprado() != null) item.setComprado(request.comprado());
        return ItemResponse.de(itens.save(item));
    }

    @Transactional
    public void excluirItem(Long listaId, Long itemId) {
        Lista lista = obterLista(listaId); validarAberta(lista);
        Item item = itens.findById(itemId).filter(i -> i.getLista().getId().equals(listaId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado."));
        itens.delete(item);
    }

    private Lista obterLista(Long id) {
        return listas.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lista não encontrada."));
    }
    private void validarAberta(Lista lista) {
        if ("CONCLUIDA".equals(lista.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "A lista já foi concluída.");
    }
    private ListaResponse resposta(Lista lista) {
        List<ItemResponse> itensDaLista = itens.findByLista_IdOrderByIdAsc(lista.getId()).stream().map(ItemResponse::de).toList();
        return ListaResponse.de(lista, itensDaLista);
    }
}
