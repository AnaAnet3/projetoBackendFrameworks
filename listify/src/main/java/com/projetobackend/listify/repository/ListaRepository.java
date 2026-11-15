package com.projetobackend.listify.repository;

import com.projetobackend.listify.model.Lista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListaRepository extends JpaRepository<Lista, Long> {

    List<Lista> findAllByOrderByCriadaEmDesc();
}
