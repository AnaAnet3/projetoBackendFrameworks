package com.listify.backend.repository;
import com.listify.backend.model.HistoricoLista;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface HistoricoListaRepository extends JpaRepository<HistoricoLista, Long> {
    List<HistoricoLista> findAllByOrderByConcluidaEmDesc();
}
