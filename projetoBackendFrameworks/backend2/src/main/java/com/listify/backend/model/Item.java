package com.listify.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "itens")
public class Item {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lista_id", nullable = false)
    private Lista lista;
    @Column(nullable = false, length = 160)
    private String nome;
    @Column(length = 60)
    private String quantidade;
    @Column(nullable = false)
    private boolean comprado = false;

    public Long getId() { return id; }
    public Lista getLista() { return lista; }
    public void setLista(Lista lista) { this.lista = lista; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getQuantidade() { return quantidade; }
    public void setQuantidade(String quantidade) { this.quantidade = quantidade; }
    public boolean isComprado() { return comprado; }
    public void setComprado(boolean comprado) { this.comprado = comprado; }
}
