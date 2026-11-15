package com.projetobackend.listify.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "listas")
public class Lista {

    public static final String ATIVA = "ATIVA";
    public static final String CONCLUIDA = "CONCLUIDA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, length = 15)
    private String status = ATIVA;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;

    /** Orçamento da lista (opcional). Null = sem limite. */
    @Column(precision = 10, scale = 2)
    private BigDecimal limite;

    /** Modelo (ex.: "Compra do mês"): não é comprado nem concluído, só serve para gerar listas novas. */
    @Column(nullable = false)
    private boolean modelo = false;

    @PrePersist
    void aoCriar() {
        if (criadaEm == null) criadaEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public LocalDateTime getConcluidaEm() { return concluidaEm; }
    public void setConcluidaEm(LocalDateTime concluidaEm) { this.concluidaEm = concluidaEm; }
    public BigDecimal getLimite() { return limite; }
    public void setLimite(BigDecimal limite) {
        this.limite = limite == null ? null : limite.setScale(2, RoundingMode.HALF_UP);
    }
    public boolean isModelo() { return modelo; }
    public void setModelo(boolean modelo) { this.modelo = modelo; }
}
