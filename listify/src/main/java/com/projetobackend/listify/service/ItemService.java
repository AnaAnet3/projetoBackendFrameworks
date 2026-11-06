package com.projetobackend.listify.service;

import com.projetobackend.listify.dto.ItemDtos.*;
import com.projetobackend.listify.exception.RecursoNaoEncontradoException;
import com.projetobackend.listify.model.*;
import com.projetobackend.listify.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ItemService {

    private final ItemListaRepository itemRepo;
    private final ListaRepository listaRepo;
    private final ProdutoRepository produtoRepo;
    private final CategoriaRepository categoriaRepo;

    public ItemService(ItemListaRepository itemRepo, ListaRepository listaRepo,
                       ProdutoRepository produtoRepo, CategoriaRepository categoriaRepo) {
        this.itemRepo = itemRepo;
        this.listaRepo = listaRepo;
        this.produtoRepo = produtoRepo;
        this.categoriaRepo = categoriaRepo;
    }

    @Transactional
    public ItemResponse adicionar(Long listaId, ItemRequest req) {
        Lista lista = buscarLista(listaId);
        Produto produto = produtoRepo.findByNomeIgnoreCase(req.produto().trim())
                .orElseGet(() -> criarProduto(req));

        ItemLista item = new ItemLista();
        item.setLista(lista);
        item.setProduto(produto);
        item.setQuantidade(req.quantidade());
        item.setUnidade(req.unidade());
        item.setPrecoEstimado(req.precoEstimado());
        return ItemResponse.de(itemRepo.save(item));
    }

    @Transactional(readOnly = true)
    public List<GrupoCategoria> listar(Long listaId) {
        buscarLista(listaId);
        return itemRepo.findByListaIdOrderByProdutoNomeAsc(listaId).stream()
                .map(ItemResponse::de)
                .collect(Collectors.groupingBy(ItemResponse::categoria, TreeMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> new GrupoCategoria(e.getKey(), e.getValue()))
                .toList();
    }

    @Transactional
    public ItemResponse editar(Long listaId, Long itemId, ItemUpdateRequest req) {
        ItemLista item = buscarItem(listaId, itemId);
        if (req.quantidade() != null) item.setQuantidade(req.quantidade());
        if (req.unidade() != null) item.setUnidade(req.unidade());
        if (req.precoEstimado() != null) item.setPrecoEstimado(req.precoEstimado());
        if (req.precoPago() != null) item.setPrecoPago(req.precoPago());
        return ItemResponse.de(item);
    }

    @Transactional
    public void remover(Long listaId, Long itemId) {
        itemRepo.delete(buscarItem(listaId, itemId));
    }

    /** Marcar: registra o preço pago (se não vier, usa o estimado). Desmarcar: limpa o preço pago. */
    @Transactional
    public ItemResponse marcarComprado(Long listaId, Long itemId, CompradoRequest req) {
        ItemLista item = buscarItem(listaId, itemId);
        item.setComprado(req.comprado());
        if (req.comprado()) {
            item.setPrecoPago(req.precoPago() != null ? req.precoPago() : item.getPrecoEstimado());
        } else {
            item.setPrecoPago(null);
        }
        return ItemResponse.de(item);
    }

    /** Estimado = soma(quantidade × preço estimado). Gasto = soma(quantidade × preço pago) dos comprados. */
    @Transactional(readOnly = true)
    public TotalResponse total(Long listaId) {
        buscarLista(listaId);
        List<ItemLista> itens = itemRepo.findByListaIdOrderByProdutoNomeAsc(listaId);

        BigDecimal estimado = itens.stream()
                .map(i -> subtotal(i.getQuantidade(), i.getPrecoEstimado()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gasto = itens.stream()
                .filter(ItemLista::isComprado)
                .map(i -> subtotal(i.getQuantidade(), i.getPrecoPago()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long comprados = itens.stream().filter(ItemLista::isComprado).count();

        return new TotalResponse(estimado.setScale(2, RoundingMode.HALF_UP),
                gasto.setScale(2, RoundingMode.HALF_UP), comprados, itens.size());
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarCategorias() {
        return categoriaRepo.findAll().stream()
                .map(c -> new CategoriaResponse(c.getId(), c.getNome()))
                .toList();
    }

    // ---------- auxiliares ----------

    private Produto criarProduto(ItemRequest req) {
        if (req.categoriaId() == null) {
            throw new IllegalArgumentException("Informe a categoria para cadastrar um produto novo.");
        }
        Categoria categoria = categoriaRepo.findById(req.categoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada: " + req.categoriaId()));
        Produto produto = new Produto();
        produto.setNome(req.produto().trim());
        produto.setCategoria(categoria);
        return produtoRepo.save(produto);
    }

    private Lista buscarLista(Long listaId) {
        return listaRepo.findById(listaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lista não encontrada: " + listaId));
    }

    private ItemLista buscarItem(Long listaId, Long itemId) {
        return itemRepo.findByIdAndListaId(itemId, listaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado: " + itemId));
    }

    private BigDecimal subtotal(BigDecimal quantidade, BigDecimal precoUnitario) {
        return precoUnitario == null ? BigDecimal.ZERO : quantidade.multiply(precoUnitario);
    }
}
