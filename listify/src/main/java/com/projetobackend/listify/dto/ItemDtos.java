package com.projetobackend.listify.dto;

import com.projetobackend.listify.model.ItemLista;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class ItemDtos {

    private ItemDtos() {}

    private static final String UNIDADES = "kg|g|L|un|pacote";
    private static final String MSG_UNIDADE = "Unidade deve ser kg, g, L, un ou pacote";

    /** Entrada: adicionar item. categoriaId só é necessário se o produto ainda não existir. */
    public record ItemRequest(
            @NotBlank(message = "Informe o produto") String produto,
            Long categoriaId,
            @NotNull(message = "Informe a quantidade")
            @Positive(message = "Quantidade deve ser maior que zero") BigDecimal quantidade,
            @NotBlank(message = "Informe a unidade")
            @Pattern(regexp = UNIDADES, message = MSG_UNIDADE) String unidade,
            @PositiveOrZero(message = "Preço não pode ser negativo") BigDecimal precoEstimado) {}

    /** Entrada: editar item (só os campos enviados são alterados). */
    public record ItemUpdateRequest(
            @Positive(message = "Quantidade deve ser maior que zero") BigDecimal quantidade,
            @Pattern(regexp = UNIDADES, message = MSG_UNIDADE) String unidade,
            @PositiveOrZero(message = "Preço não pode ser negativo") BigDecimal precoEstimado,
            @PositiveOrZero(message = "Preço não pode ser negativo") BigDecimal precoPago) {}

    /** Entrada: marcar/desmarcar como comprado. */
    public record CompradoRequest(
            @NotNull(message = "Informe se o item foi comprado") Boolean comprado,
            @PositiveOrZero(message = "Preço não pode ser negativo") BigDecimal precoPago) {}

    /** Saída: um item. */
    public record ItemResponse(Long id, String produto, String categoria, BigDecimal quantidade,
                               String unidade, BigDecimal precoEstimado, BigDecimal precoPago,
                               boolean comprado) {
        public static ItemResponse de(ItemLista i) {
            return new ItemResponse(i.getId(), i.getProduto().getNome(),
                    i.getProduto().getCategoria().getNome(), i.getQuantidade(), i.getUnidade(),
                    i.getPrecoEstimado(), i.getPrecoPago(), i.isComprado());
        }
    }

    /** Saída: itens agrupados por categoria (para a tela de detalhe). */
    public record GrupoCategoria(String categoria, List<ItemResponse> itens) {}

    /** Saída: totais da lista. */
    public record TotalResponse(BigDecimal totalEstimado, BigDecimal totalGasto,
                                long itensComprados, long totalItens) {}

    /** Saída: categoria (para o select da tela de adicionar item). */
    public record CategoriaResponse(Long id, String nome) {}
}
