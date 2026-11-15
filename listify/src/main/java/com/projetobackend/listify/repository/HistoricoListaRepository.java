package com.projetobackend.listify.repository;

import com.projetobackend.listify.model.HistoricoLista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HistoricoListaRepository extends JpaRepository<HistoricoLista, Long> {

    List<HistoricoLista> findAllByOrderByConcluidaEmDesc();

    /** Traz a lista junto (join fetch) para não fazer uma consulta extra por registro. */
    @Query("select h from HistoricoLista h join fetch h.lista order by h.concluidaEm desc")
    List<HistoricoLista> findAllComLista();
}
