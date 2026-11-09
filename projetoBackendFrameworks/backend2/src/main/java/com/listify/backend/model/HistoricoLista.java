package com.listify.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historico_listas")
public class HistoricoLista {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lista_id", nullable = false)
    private Lista lista;
    @Column(name = "nome_lista", nullable = false, length = 120)
    private String nomeLista;
    @Column(name = "concluida_em", nullable = false)
    private LocalDateTime concluidaEm;

    protected HistoricoLista() {}
    public HistoricoLista(Lista lista, String nomeLista, LocalDateTime concluidaEm) {
        this.lista = lista; this.nomeLista = nomeLista; this.concluidaEm = concluidaEm;
    }
    public Long getId() { return id; }
    public Long getListaId() { return lista.getId(); }
    public String getNomeLista() { return nomeLista; }
    public LocalDateTime getConcluidaEm() { return concluidaEm; }
}
