package com.projetobackend.listify.dto;

import com.projetobackend.listify.model.HistoricoLista;
import com.projetobackend.listify.model.ItemLista;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Uma compra concluída, com os totais já calculados para a tela de histórico.
 *
 * totalEstimadoComprados considera só os itens que foram comprados; é com ele
 * que se compara o gasto para saber quanto se economizou (itens que ficaram de
 * fora da compra não entram na conta).
 */
public record HistoricoResponse(Long id, Long listaId, String nomeLista,
                                LocalDateTime criadaEm, LocalDateTime concluidaEm,
                                long totalItens, long itensComprados,
                                BigDecimal totalEstimado, BigDecimal totalEstimadoComprados,
                                BigDecimal totalGasto) {

    public static HistoricoResponse de(HistoricoLista historico, List<ItemLista> itens) {
        BigDecimal estimado = BigDecimal.ZERO;
        BigDecimal estimadoComprados = BigDecimal.ZERO;
        BigDecimal gasto = BigDecimal.ZERO;
        long comprados = 0;

        for (ItemLista item : itens) {
            BigDecimal subtotalEstimado = subtotal(item.getQuantidade(), item.getPrecoEstimado());
            estimado = estimado.add(subtotalEstimado);

            if (item.isComprado()) {
                comprados++;
                estimadoComprados = estimadoComprados.add(subtotalEstimado);
                gasto = gasto.add(subtotal(item.getQuantidade(), item.getPrecoPago()));
            }
        }

        return new HistoricoResponse(
                historico.getId(), historico.getListaId(), historico.getNomeLista(),
                historico.getLista().getCriadaEm(), historico.getConcluidaEm(),
                itens.size(), comprados,
                arredondar(estimado), arredondar(estimadoComprados), arredondar(gasto));
    }

    private static BigDecimal subtotal(BigDecimal quantidade, BigDecimal precoUnitario) {
        return precoUnitario == null ? BigDecimal.ZERO : quantidade.multiply(precoUnitario);
    }

    private static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
