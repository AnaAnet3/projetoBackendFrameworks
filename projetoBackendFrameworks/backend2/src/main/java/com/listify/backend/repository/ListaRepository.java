package com.listify.backend.repository;
import com.listify.backend.model.Lista;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ListaRepository extends JpaRepository<Lista, Long> {}
