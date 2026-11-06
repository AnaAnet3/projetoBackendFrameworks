package com.projetobackend.listify.repository;

import com.projetobackend.listify.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Optional<Produto> findByNomeIgnoreCase(String nome);
}
