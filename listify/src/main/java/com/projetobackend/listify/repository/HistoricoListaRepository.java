package com.projetobackend.listify.repository;

import com.projetobackend.listify.model.HistoricoLista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoListaRepository extends JpaRepository<HistoricoLista, Long> {

    List<HistoricoLista> findAllByOrderByConcluidaEmDesc();
}
