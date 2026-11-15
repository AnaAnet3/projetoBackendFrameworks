package com.projetobackend.listify.repository;

import com.projetobackend.listify.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Optional<Produto> findByNomeIgnoreCase(String nome);

    @Query(value = "SELECT ultimo_preco FROM vw_ultimo_preco_produto WHERE produto_id = :produtoId",
            nativeQuery = true)
    Optional<BigDecimal> findUltimoPreco(@Param("produtoId") Long produtoId);

    @Query(value = """
            SELECT p.id AS id, p.nome AS nome, c.id AS categoriaId, c.nome AS categoria,
                   p.unidade_padrao AS unidade, u.ultimo_preco AS ultimoPreco
            FROM produtos p
            JOIN categorias c ON c.id = p.categoria_id
            LEFT JOIN vw_ultimo_preco_produto u ON u.produto_id = p.id
            WHERE LOWER(p.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
            ORDER BY p.nome
            LIMIT 8
            """, nativeQuery = true)
    List<ProdutoSugestao> buscarSugestoes(@Param("busca") String busca);

    interface ProdutoSugestao {
        Long getId();
        String getNome();
        Long getCategoriaId();
        String getCategoria();
        String getUnidade();
        BigDecimal getUltimoPreco();
    }
}
