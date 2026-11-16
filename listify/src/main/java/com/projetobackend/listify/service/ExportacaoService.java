package com.projetobackend.listify.service;

import com.projetobackend.listify.dto.ItemDtos.GrupoCategoria;
import com.projetobackend.listify.dto.ItemDtos.ItemResponse;
import com.projetobackend.listify.dto.ItemDtos.TotalResponse;
import com.projetobackend.listify.dto.ListaResponse;
import com.projetobackend.listify.model.ItemLista;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * Gera o texto de uma lista, pronto para copiar e colar onde a pessoa quiser.
 * Os itens saem agrupados por categoria. O PDF é gerado no navegador (imprimir → salvar como PDF).
 */
@Service
public class ExportacaoService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final ListaService listaService;
    private final ItemService itemService;

    public ExportacaoService(ListaService listaService, ItemService itemService) {
        this.listaService = listaService;
        this.itemService = itemService;
    }

    @Transactional(readOnly = true)
    public String texto(Long listaId) {
        ListaResponse lista = listaService.buscar(listaId);
        return montarTexto(lista, itemService.listar(listaId), itemService.total(listaId));
    }

    // Método estático: não depende do banco, fácil de testar.
    static String montarTexto(ListaResponse lista, List<GrupoCategoria> grupos, TotalResponse total) {
        StringBuilder texto = new StringBuilder();
        texto.append("🛒 *").append(lista.nome()).append("*\n");
        if (lista.limite() != null) {
            texto.append("Orçamento: ").append(real(lista.limite())).append('\n');
        }

        for (GrupoCategoria grupo : grupos) {
            texto.append("\n*").append(grupo.categoria()).append("*\n");
            for (ItemResponse item : grupo.itens()) {
                texto.append(item.comprado() ? "✅ " : "⬜ ")
                        .append(quantidade(item.quantidade())).append(' ')
                        .append(unidadeCurta(item.unidade())).append(' ')
                        .append(item.produto());
                if (item.marca() != null) texto.append(" (").append(item.marca()).append(')');
                if (ItemLista.ESSENCIAL.equals(item.prioridade())) texto.append(" ❗");
                if (ItemLista.IMPORTANTE.equals(item.prioridade())) texto.append(" ⭐");
                if (item.observacao() != null) texto.append(" - ").append(item.observacao());
                texto.append('\n');
            }
        }

        texto.append("\nTotal estimado: ").append(real(total.totalEstimado()));
        if (total.itensComprados() > 0) {
            texto.append("\nGasto até agora: ").append(real(total.totalGasto()));
        }
        return texto.toString();
    }

    private static String quantidade(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static String real(BigDecimal valor) {
        return "R$ " + String.format(PT_BR, "%,.2f", valor);
    }

    private static String unidadeCurta(String unidade) {
        return "pacote".equals(unidade) ? "pct" : unidade;
    }
}
