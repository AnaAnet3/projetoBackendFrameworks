package com.projetobackend.listify.service;

import com.projetobackend.listify.dto.ListaRequest;
import com.projetobackend.listify.dto.HistoricoResponse;
import com.projetobackend.listify.dto.ListaResponse;
import com.projetobackend.listify.exception.ConflitoException;
import com.projetobackend.listify.exception.RecursoNaoEncontradoException;
import com.projetobackend.listify.model.HistoricoLista;
import com.projetobackend.listify.model.ItemLista;
import com.projetobackend.listify.model.Lista;
import com.projetobackend.listify.repository.HistoricoListaRepository;
import com.projetobackend.listify.repository.ItemListaRepository;
import com.projetobackend.listify.repository.ListaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Regras de negócio das listas (adaptado da backend2 para o modelo com produtos e preços). */
@Service
public class ListaService {

    private final ListaRepository listas;
    private final ItemListaRepository itens;
    private final HistoricoListaRepository historico;

    public ListaService(ListaRepository listas, ItemListaRepository itens, HistoricoListaRepository historico) {
        this.listas = listas;
        this.itens = itens;
        this.historico = historico;
    }

    @Transactional(readOnly = true)
    public List<ListaResponse> listar() {
        return listas.findAllByOrderByCriadaEmDesc().stream().map(ListaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ListaResponse buscar(Long id) {
        return ListaResponse.de(obterLista(id));
    }

    @Transactional
    public ListaResponse criar(ListaRequest request) {
        Lista lista = new Lista();
        lista.setNome(request.nome().trim());
        return ListaResponse.de(listas.save(lista));
    }

    @Transactional
    public ListaResponse editar(Long id, ListaRequest request) {
        Lista lista = obterLista(id);
        validarAberta(lista, "Não é possível editar uma lista concluída.");
        lista.setNome(request.nome().trim());
        return ListaResponse.de(listas.save(lista));
    }

    /** Os itens e o histórico saem junto, via ON DELETE CASCADE no banco. */
    @Transactional
    public void excluir(Long id) {
        listas.delete(obterLista(id));
    }

    /** Cópia nova e ativa; todos os itens voltam como "não comprados" e sem preço pago. */
    @Transactional
    public ListaResponse duplicar(Long id) {
        Lista original = obterLista(id);
        Lista copia = new Lista();
        String nome = original.getNome();
        if (nome.length() > 240) nome = nome.substring(0, 240);
        copia.setNome(nome + " (cópia)");
        copia = listas.save(copia);

        for (ItemLista antigo : itens.findByListaIdOrderByProdutoNomeAsc(id)) {
            ItemLista novo = new ItemLista();
            novo.setLista(copia);
            novo.setProduto(antigo.getProduto());
            novo.setQuantidade(antigo.getQuantidade());
            novo.setUnidade(antigo.getUnidade());
            novo.setPrecoEstimado(antigo.getPrecoEstimado());
            novo.setPrecoPago(null);
            novo.setComprado(false);
            itens.save(novo);
        }
        return ListaResponse.de(copia);
    }

    /** Conclui a lista e registra no histórico. */
    @Transactional
    public ListaResponse concluir(Long id) {
        Lista lista = obterLista(id);
        if (!Lista.CONCLUIDA.equals(lista.getStatus())) {
            LocalDateTime agora = LocalDateTime.now();
            lista.setStatus(Lista.CONCLUIDA);
            lista.setConcluidaEm(agora);
            listas.save(lista);
            historico.save(new HistoricoLista(lista, lista.getNome(), agora));
        }
        return ListaResponse.de(lista);
    }

    /** Compras concluídas (da mais recente para a mais antiga), com os totais de cada uma. */
    @Transactional(readOnly = true)
    public List<HistoricoResponse> historico() {
        List<HistoricoLista> registros = historico.findAllComLista();
        if (registros.isEmpty()) {
            return List.of();
        }

        // uma única consulta para os itens de todas as listas do histórico
        List<Long> listaIds = registros.stream().map(HistoricoLista::getListaId).toList();
        Map<Long, List<ItemLista>> itensPorLista = itens.findByListaIdIn(listaIds).stream()
                .collect(Collectors.groupingBy(item -> item.getLista().getId()));

        return registros.stream()
                .map(h -> HistoricoResponse.de(h, itensPorLista.getOrDefault(h.getListaId(), List.of())))
                .toList();
    }

    private Lista obterLista(Long id) {
        return listas.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lista não encontrada."));
    }

    private void validarAberta(Lista lista, String mensagem) {
        if (Lista.CONCLUIDA.equals(lista.getStatus())) {
            throw new ConflitoException(mensagem);
        }
    }
}
