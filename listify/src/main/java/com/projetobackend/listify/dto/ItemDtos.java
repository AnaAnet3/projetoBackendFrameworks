package com.projetobackend.listify.dto;

import com.projetobackend.listify.model.ItemLista;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class ItemDtos {

    private ItemDtos() {}

    private static final String UNIDADES = "kg|g|L|un|pacote";
    private static final String MSG_UNIDADE = "Unidade deve ser kg, g, L, un ou pacote";
    private static final String PRIORIDADES = "NORMAL|IMPORTANTE|ESSENCIAL";
    private static final String MSG_PRIORIDADE = "Prioridade deve ser NORMAL, IMPORTANTE ou ESSENCIAL";

    public record ItemRequest(
            @NotBlank(message = "Informe o produto") String produto,
            Long categoriaId,
            @NotNull(message = "Informe a quantidade")
            @Positive(message = "Quantidade deve ser maior que zero") BigDecimal quantidade,
            @NotBlank(message = "Informe a unidade")
            @Pattern(regexp = UNIDADES, message = MSG_UNIDADE) String unidade,
            @PositiveOrZero(message = "Preço não pode ser negativo") BigDecimal precoEstimado,
            @Size(max = 60, message = "A marca pode ter no máximo 60 caracteres") String marca,
            @Size(max = 255, message = "A observação pode ter no máximo 255 caracteres") String observacao,
            @Pattern(regexp = PRIORIDADES, message = MSG_PRIORIDADE) String prioridade) {}


    public record ItemUpdateRequest(
            @Positive(message = "Quantidade deve ser maior que zero") BigDecimal quantidade,
            @Pattern(regexp = UNIDADES, message = MSG_UNIDADE) String unidade,
            @PositiveOrZero(message = "Preço não pode ser negativo") BigDecimal precoEstimado,
            @PositiveOrZero(message = "Preço não pode ser negativo") BigDecimal precoPago,
            @Size(max = 60, message = "A marca pode ter no máximo 60 caracteres") String marca,
            @Size(max = 255, message = "A observação pode ter no máximo 255 caracteres") String observacao,
            @Pattern(regexp = PRIORIDADES, message = MSG_PRIORIDADE) String prioridade) {}

    public record CompradoRequest(
            @NotNull(message = "Informe se o item foi comprado") Boolean comprado,
            @PositiveOrZero(message = "Preço não pode ser negativo") BigDecimal precoPago) {}

    public record ItemResponse(Long id, String produto, String categoria, BigDecimal quantidade,
                               String unidade, BigDecimal precoEstimado, BigDecimal precoPago,
                               boolean comprado, String marca, String observacao, String prioridade) {
        public static ItemResponse de(ItemLista i) {
            return new ItemResponse(i.getId(), i.getProduto().getNome(),
                    i.getProduto().getCategoria().getNome(), i.getQuantidade(), i.getUnidade(),
                    i.getPrecoEstimado(), i.getPrecoPago(), i.isComprado(),
                    i.getMarca(), i.getObservacao(), i.getPrioridade());
        }
    }

    /** Saída: itens agrupados por categoria (para a tela de detalhe). */
    public record GrupoCategoria(String categoria, List<ItemResponse> itens) {}

    
    public record TotalResponse(BigDecimal totalEstimado, BigDecimal totalGasto,
                                long itensComprados, long totalItens,
                                BigDecimal limite, String situacaoOrcamento) {

        public static final String SEM_LIMITE = "SEM_LIMITE";
        public static final String DENTRO = "DENTRO";
        public static final String PERTO_DO_LIMITE = "PERTO_DO_LIMITE";
        public static final String ESTIMADO_ESTOUROU = "ESTIMADO_ESTOUROU";
        public static final String GASTO_ESTOUROU = "GASTO_ESTOUROU";

        /** Aviso a partir de 90% do limite  */
        private static final BigDecimal LIMIAR_AVISO = new BigDecimal("0.90");

        public static String situacao(BigDecimal limite, BigDecimal estimado, BigDecimal gasto) {
            if (limite == null) return SEM_LIMITE;
            if (gasto.compareTo(limite) > 0) return GASTO_ESTOUROU;
            if (estimado.compareTo(limite) > 0) return ESTIMADO_ESTOUROU;

            BigDecimal maior = estimado.max(gasto);
            if (maior.compareTo(limite.multiply(LIMIAR_AVISO)) >= 0) return PERTO_DO_LIMITE;
            return DENTRO;
        }
    }

    public record CategoriaResponse(Long id, String nome) {}
}
