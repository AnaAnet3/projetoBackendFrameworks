package com.projetobackend.listify.dto;

import java.math.BigDecimal;

public record ProdutoResponse(Long id, String nome, Long categoriaId, String categoria,
                              String unidade, BigDecimal ultimoPreco) {
}
