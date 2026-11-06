package com.projetobackend.listify.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "itens_lista")
public class ItemLista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "lista_id")
    private Lista lista;

    @ManyToOne(optional = false)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantidade;

    @Column(nullable = false, length = 10)
    private String unidade;

    // Preços unitários (por kg, un, pacote...)
    @Column(precision = 10, scale = 2)
    private BigDecimal precoEstimado;

    @Column(precision = 10, scale = 2)
    private BigDecimal precoPago;

    @Column(nullable = false)
    private boolean comprado = false;

    public Long getId() { return id; }
    public Lista getLista() { return lista; }
    public void setLista(Lista lista) { this.lista = lista; }
    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public String getUnidade() { return unidade; }
    public void setUnidade(String unidade) { this.unidade = unidade; }
    public BigDecimal getPrecoEstimado() { return precoEstimado; }
    public void setPrecoEstimado(BigDecimal precoEstimado) { this.precoEstimado = precoEstimado; }
    public BigDecimal getPrecoPago() { return precoPago; }
    public void setPrecoPago(BigDecimal precoPago) { this.precoPago = precoPago; }
    public boolean isComprado() { return comprado; }
    public void setComprado(boolean comprado) { this.comprado = comprado; }
}
