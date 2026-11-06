package com.projetobackend.listify.repository;

import com.projetobackend.listify.model.ItemLista;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ItemListaRepository extends JpaRepository<ItemLista, Long> {

    List<ItemLista> findByListaIdOrderByProdutoNomeAsc(Long listaId);

    Optional<ItemLista> findByIdAndListaId(Long id, Long listaId);
}
