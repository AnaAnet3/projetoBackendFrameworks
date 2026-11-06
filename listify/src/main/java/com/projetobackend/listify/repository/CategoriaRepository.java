package com.projetobackend.listify.repository;

import com.projetobackend.listify.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}
