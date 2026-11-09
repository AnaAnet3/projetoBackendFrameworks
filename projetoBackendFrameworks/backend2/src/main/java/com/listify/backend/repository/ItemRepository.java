package com.listify.backend.repository;
import com.listify.backend.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByLista_IdOrderByIdAsc(Long listaId);
    void deleteByLista_Id(Long listaId);
}
