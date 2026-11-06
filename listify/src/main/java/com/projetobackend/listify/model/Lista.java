package com.projetobackend.listify.model;

import jakarta.persistence.*;

/**
 * TEMPORÁRIA: só para você rodar e testar hoje.
 * Quando a pessoa 2 (Backend A) subir a Lista de verdade, apague este arquivo
 * e o ListaRepository temporário.
 */
@Entity
@Table(name = "listas")
public class Lista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}
