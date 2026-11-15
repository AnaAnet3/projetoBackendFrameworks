package com.projetobackend.listify.service;

import com.projetobackend.listify.dto.ProdutoResponse;
import com.projetobackend.listify.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepo;

    public ProdutoService(ProdutoRepository produtoRepo) {
        this.produtoRepo = produtoRepo;
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> buscar(String busca) {
        if (busca == null || busca.isBlank()) {
            return List.of();
        }
        String texto = busca.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");

        return produtoRepo.buscarSugestoes(texto).stream()
                .map(s -> new ProdutoResponse(s.getId(), s.getNome(), s.getCategoriaId(),
                        s.getCategoria(), s.getUnidade(), s.getUltimoPreco()))
                .toList();
    }
}
