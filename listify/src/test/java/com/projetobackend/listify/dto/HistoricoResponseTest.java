package com.projetobackend.listify.dto;

import com.projetobackend.listify.model.HistoricoLista;
import com.projetobackend.listify.model.ItemLista;
import com.projetobackend.listify.model.Lista;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HistoricoResponseTest {

    private HistoricoLista registro() {
        Lista lista = new Lista();
        lista.setNome("Compra do mês");
        return new HistoricoLista(lista, "Compra do mês", LocalDateTime.of(2026, 11, 10, 18, 0));
    }

    private ItemLista item(String quantidade, String estimado, String pago, boolean comprado) {
        ItemLista item = new ItemLista();
        item.setQuantidade(new BigDecimal(quantidade));
        item.setPrecoEstimado(new BigDecimal(estimado));
        item.setPrecoPago(pago == null ? null : new BigDecimal(pago));
        item.setComprado(comprado);
        return item;
    }

    private void assertValor(String esperado, BigDecimal atual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(atual), "esperado " + esperado + " mas veio " + atual);
    }

    @Test
    void gastoContaSoItensComprados() {
        HistoricoResponse r = HistoricoResponse.de(registro(), List.of(
                item("2", "6.00", "5.50", true),   // comprado: estimado 12,00 / gasto 11,00
                item("1", "8.00", null, false)));  // não comprado: só entra no estimado

        assertEquals(2, r.totalItens());
        assertEquals(1, r.itensComprados());
        assertValor("20.00", r.totalEstimado());
        assertValor("12.00", r.totalEstimadoComprados());
        assertValor("11.00", r.totalGasto());
    }

    @Test
    void listaSemItensTemTotaisZerados() {
        HistoricoResponse r = HistoricoResponse.de(registro(), List.of());

        assertEquals(0, r.totalItens());
        assertValor("0.00", r.totalEstimado());
        assertValor("0.00", r.totalGasto());
    }
}
