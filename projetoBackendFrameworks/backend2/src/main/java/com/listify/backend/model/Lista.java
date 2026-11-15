package com.listify.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "listas")
public class Lista {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String nome;
    @Column(nullable = false, length = 20)
    private String status = "ABERTA";
    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;
    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;

    @PrePersist void aoCriar() { if (criadaEm == null) criadaEm = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public LocalDateTime getConcluidaEm() { return concluidaEm; }
    public void setConcluidaEm(LocalDateTime concluidaEm) { this.concluidaEm = concluidaEm; }
}
