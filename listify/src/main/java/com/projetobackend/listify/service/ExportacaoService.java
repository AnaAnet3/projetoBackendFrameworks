package com.projetobackend.listify.service;

import com.projetobackend.listify.dto.ItemDtos.GrupoCategoria;
import com.projetobackend.listify.dto.ItemDtos.ItemResponse;
import com.projetobackend.listify.dto.ItemDtos.TotalResponse;
import com.projetobackend.listify.dto.ListaResponse;
import com.projetobackend.listify.model.ItemLista;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Exporta uma lista como CSV (para planilha) ou como texto pronto para colar/enviar no WhatsApp.
 * Os itens saem agrupados por categoria. O PDF é gerado no navegador (imprimir → salvar como PDF).
 */
@Service
public class ExportacaoService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final String SEPARADOR = ";"; // o Excel em português espera ponto e vírgula

    /** Um arquivo pronto para download. */
    public record Arquivo(String nome, byte[] conteudo) {}

    private final ListaService listaService;
    private final ItemService itemService;

    public ExportacaoService(ListaService listaService, ItemService itemService) {
        this.listaService = listaService;
        this.itemService = itemService;
    }

    @Transactional(readOnly = true)
    public Arquivo csv(Long listaId) {
        ListaResponse lista = listaService.buscar(listaId);
        String csv = montarCsv(lista, itemService.listar(listaId), itemService.total(listaId));

        // BOM no começo para o Excel reconhecer os acentos (UTF-8)
        byte[] bytes = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
        return new Arquivo(nomeDeArquivo(lista.nome()) + ".csv", bytes);
    }

    @Transactional(readOnly = true)
    public String texto(Long listaId) {
        ListaResponse lista = listaService.buscar(listaId);
        return montarTexto(lista, itemService.listar(listaId), itemService.total(listaId));
    }

    // ---------- montagem (métodos estáticos: não dependem do banco, fáceis de testar) ----------

    static String montarCsv(ListaResponse lista, List<GrupoCategoria> grupos, TotalResponse total) {
        StringBuilder csv = new StringBuilder();
        linha(csv, "Categoria", "Produto", "Marca", "Quantidade", "Unidade", "Preço estimado",
                "Preço pago", "Subtotal", "Prioridade", "Observação", "Comprado");

        for (GrupoCategoria grupo : grupos) {
            for (ItemResponse item : grupo.itens()) {
                linha(csv, grupo.categoria(), item.produto(), item.marca(),
                        quantidade(item.quantidade()), item.unidade(),
                        dinheiro(item.precoEstimado()), dinheiro(item.precoPago()),
                        dinheiro(subtotal(item)), rotuloPrioridade(item.prioridade()),
                        item.observacao(), item.comprado() ? "Sim" : "Não");
            }
        }

        linha(csv, "TOTAL ESTIMADO", "", "", "", "", "", "", dinheiro(total.totalEstimado()), "", "", "");
        linha(csv, "TOTAL GASTO", "", "", "", "", "", "", dinheiro(total.totalGasto()), "", "", "");
        if (lista.limite() != null) {
            linha(csv, "LIMITE", "", "", "", "", "", "", dinheiro(lista.limite()), "", "", "");
        }
        return csv.toString();
    }

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

    // ---------- auxiliares ----------

    private static void linha(StringBuilder csv, String... celulas) {
        for (int i = 0; i < celulas.length; i++) {
            if (i > 0) csv.append(SEPARADOR);
            csv.append(celula(celulas[i]));
        }
        csv.append("\r\n");
    }

    /** Escapa a célula; textos que começam com = + - @ ganham um apóstrofo para o Excel não tratar como fórmula. */
    private static String celula(String valor) {
        if (valor == null || valor.isEmpty()) return "";
        String v = valor;
        if ("=+-@".indexOf(v.charAt(0)) >= 0) v = "'" + v;
        if (v.contains(SEPARADOR) || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    /** Item comprado usa o preço pago; senão, o estimado. */
    private static BigDecimal subtotal(ItemResponse item) {
        BigDecimal preco = item.comprado() && item.precoPago() != null ? item.precoPago() : item.precoEstimado();
        return preco == null ? BigDecimal.ZERO : item.quantidade().multiply(preco);
    }

    private static String quantidade(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static String dinheiro(BigDecimal valor) {
        return valor == null ? "" : String.format(PT_BR, "%.2f", valor);
    }

    private static String real(BigDecimal valor) {
        return "R$ " + String.format(PT_BR, "%,.2f", valor);
    }

    private static String unidadeCurta(String unidade) {
        return "pacote".equals(unidade) ? "pct" : unidade;
    }

    private static String rotuloPrioridade(String prioridade) {
        if (ItemLista.ESSENCIAL.equals(prioridade)) return "Essencial";
        if (ItemLista.IMPORTANTE.equals(prioridade)) return "Importante";
        return "Normal";
    }

    /** "Compra do mês!" vira "compra-do-mes". */
    static String nomeDeArquivo(String nome) {
        String semAcento = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "lista" : slug;
    }
}
