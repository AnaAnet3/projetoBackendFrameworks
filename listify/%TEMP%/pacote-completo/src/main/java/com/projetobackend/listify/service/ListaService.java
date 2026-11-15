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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        lista.setLimite(request.limite());
        return ListaResponse.de(listas.save(lista));
    }

    @Transactional
    public ListaResponse editar(Long id, ListaRequest request) {
        Lista lista = obterLista(id);
        validarAberta(lista, "Não é possível editar uma lista concluída.");
        lista.setNome(request.nome().trim());
        lista.setLimite(request.limite());
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
        Lista copia = novaListaBaseadaEm(original, nomeComSufixo(original.getNome(), " (cópia)"), false);
        copiarItens(original, copia);
        return ListaResponse.de(copia);
    }

    /** Guarda uma cópia da lista como modelo (ex.: "Compra do mês"), com os mesmos itens e limite. */
    @Transactional
    public ListaResponse salvarComoModelo(Long id) {
        Lista original = obterLista(id);
        if (original.isModelo()) {
            throw new ConflitoException("Esta lista já é um modelo.");
        }
        Lista modelo = novaListaBaseadaEm(original, nomeComSufixo(original.getNome(), ""), true);
        copiarItens(original, modelo);
        return ListaResponse.de(modelo);
    }

    /** Gera uma lista nova e ativa a partir de um modelo, com a data de hoje no nome. */
    @Transactional
    public ListaResponse usarModelo(Long id) {
        Lista modelo = obterLista(id);
        if (!modelo.isModelo()) {
            throw new ConflitoException("Esta lista não é um modelo.");
        }
        String data = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        Lista nova = novaListaBaseadaEm(modelo, nomeComSufixo(modelo.getNome(), " - " + data), false);
        copiarItens(modelo, nova);
        return ListaResponse.de(nova);
    }

    /** Conclui a lista e registra no histórico. */
    @Transactional
    public ListaResponse concluir(Long id) {
        Lista lista = obterLista(id);
        if (lista.isModelo()) {
            throw new ConflitoException("Um modelo não pode ser concluído. Use-o para criar uma lista nova.");
        }
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

    /** Lista nova (salva) com o limite da original; modelo diz se a nova também é um modelo. */
    private Lista novaListaBaseadaEm(Lista original, String nome, boolean modelo) {
        Lista nova = new Lista();
        nova.setNome(nome);
        nova.setLimite(original.getLimite());
        nova.setModelo(modelo);
        return listas.save(nova);
    }

    /** Copia os itens (produto, quantidade, preço estimado, marca, observação e prioridade) sem o estado de compra. */
    private void copiarItens(Lista origem, Lista destino) {
        for (ItemLista antigo : itens.findByListaIdOrderByProdutoNomeAsc(origem.getId())) {
            ItemLista novo = new ItemLista();
            novo.setLista(destino);
            novo.setProduto(antigo.getProduto());
            novo.setQuantidade(antigo.getQuantidade());
            novo.setUnidade(antigo.getUnidade());
            novo.setPrecoEstimado(antigo.getPrecoEstimado());
            novo.setMarca(antigo.getMarca());
            novo.setObservacao(antigo.getObservacao());
            novo.setPrioridade(antigo.getPrioridade());
            novo.setPrecoPago(null);
            novo.setComprado(false);
            itens.save(novo);
        }
    }

    /** Garante que nome + sufixo cabem na coluna (255). */
    private String nomeComSufixo(String nome, String sufixo) {
        int maximo = 255 - sufixo.length();
        String base = nome.length() > maximo ? nome.substring(0, maximo) : nome;
        return base + sufixo;
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
