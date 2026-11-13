package com.energiasolar.web;

import com.energiasolar.model.Transacao;
import com.energiasolar.model.Usuario;
import com.energiasolar.service.RelatorioService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

public class RelatorioHandler implements HttpHandler {

    private final RelatorioService relatorioService = new RelatorioService();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        Usuario usuario = Autenticacao.usuarioLogado(ex);
        if (usuario == null) { Http.redirecionar(ex, "/login"); return; }

        StringBuilder linhasCentro = new StringBuilder();
        double totalKwhGeral = 0, totalFaturadoGeral = 0;
        int transacoesGeral = 0;
        for (Object[] linha : relatorioService.relatorioPorCentroDistribuicao()) {
            String nome = (String) linha[0];
            double totalKwh = (double) linha[1];
            double totalFaturado = (double) linha[2];
            int qtdTransacoes = (int) linha[3];
            totalKwhGeral += totalKwh;
            totalFaturadoGeral += totalFaturado;
            transacoesGeral += qtdTransacoes;
            linhasCentro.append("""
                <tr><td>%s</td><td class="num">%.2f kWh</td><td class="num">R$ %.2f</td><td class="num">%d</td></tr>
                """.formatted(Http.escapar(nome), totalKwh, totalFaturado, qtdTransacoes));
        }

        List<Transacao> compras = relatorioService.extratoComprasDoUsuario(usuario.getId());
        List<Transacao> vendas = relatorioService.extratoVendasDoUsuario(usuario.getId());
        double kwhComprado = somaKwh(compras), pago = somaValor(compras);
        double kwhVendido = somaKwh(vendas), faturado = somaValor(vendas);

        boolean vende = usuario.podeOfertar(), compra = usuario.podeComprar();
        StringBuilder indicadores = new StringBuilder();
        String subtitulo;
        if (vende && compra) {
            subtitulo = "As suas vendas e compras no marketplace e a movimentação por centro de distribuição.";
            indicadores.append(indicador(true, "%.2f<small>kWh</small>".formatted(kwhVendido), "Energia vendida, R$ %.2f faturados".formatted(faturado)));
            indicadores.append(indicador(false, "%.2f<small>kWh</small>".formatted(kwhComprado), "Energia comprada, R$ %.2f pagos".formatted(pago)));
            indicadores.append(indicador(false, "R$ %.2f".formatted(faturado - pago), "Saldo entre vendas e compras"));
        } else if (vende) {
            subtitulo = "As suas vendas no marketplace e a movimentação por centro de distribuição.";
            indicadores.append(indicador(true, "%.2f<small>kWh</small>".formatted(kwhVendido), "Energia vendida"));
            indicadores.append(indicador(false, "R$ %.2f".formatted(faturado), "Faturamento das suas vendas"));
            indicadores.append(indicador(false, String.valueOf(vendas.size()), vendas.size() == 1 ? "Venda realizada" : "Vendas realizadas"));
        } else {
            subtitulo = "As suas compras no marketplace e a movimentação por centro de distribuição.";
            indicadores.append(indicador(true, "%.2f<small>kWh</small>".formatted(kwhComprado), "Energia comprada"));
            indicadores.append(indicador(false, "R$ %.2f".formatted(pago), "Total pago pelas compras"));
            indicadores.append(indicador(false, kwhComprado > 0 ? "R$ %.2f".formatted(pago / kwhComprado) : "-", "Preço médio pago por kWh"));
        }

        String extratos;
        if (vende && compra) {
            extratos = """
                <div class="grade-meia secao">
                    <section><div class="secao-topo"><h3>Meu extrato de vendas</h3></div>%s</section>
                    <section><div class="secao-topo"><h3>Meu extrato de compras</h3></div>%s</section>
                </div>
                """.formatted(extrato(vendas, "Comprador", false), extrato(compras, "Vendedor", true));
        } else {
            extratos = """
                <section class="secao"><div class="secao-topo"><h3>%s</h3></div>%s</section>
                """.formatted(vende ? "Meu extrato de vendas" : "Meu extrato de compras",
                    vende ? extrato(vendas, "Comprador", false) : extrato(compras, "Vendedor", true));
        }

        String conteudo = """
            <div class="cabecalho-pagina">
                <h2>Relatórios</h2>
                <p>%s</p>
            </div>
            <div class="indicadores">%s</div>
            %s
            <section class="secao">
                <div class="secao-topo"><div><h3>Movimentação por centro de distribuição</h3><p>Todas as negociações do marketplace, de todos os participantes.</p></div></div>
                <div class="tabela-envoltorio"><table>
                    <thead><tr><th>Centro</th><th class="num">Total negociado</th><th class="num">Faturamento</th><th class="num">Transações</th></tr></thead>
                    <tbody>%s</tbody>
                    <tfoot><tr><td>Total geral</td><td class="num">%.2f kWh</td><td class="num">R$ %.2f</td><td class="num">%d</td></tr></tfoot>
                </table></div>
            </section>
            """.formatted(subtitulo, indicadores, extratos,
                linhasCentro, totalKwhGeral, totalFaturadoGeral, transacoesGeral);

        Http.enviarHtml(ex, 200, Layout.pagina("Relatórios", usuario, "/relatorios", conteudo));
    }

    private static String indicador(boolean destaque, String valor, String descricao) {
        return "<div class=\"indicador%s\"><span class=\"valor\">%s</span><span class=\"descricao\">%s</span></div>"
                .formatted(destaque ? " destaque" : "", valor, descricao);
    }

    private static double somaKwh(List<Transacao> transacoes) {
        double total = 0;
        for (Transacao t : transacoes) total += t.getQuantidadeKwh();
        return total;
    }

    private static double somaValor(List<Transacao> transacoes) {
        double total = 0;
        for (Transacao t : transacoes) total += t.getValorTotal();
        return total;
    }

    private String extrato(List<Transacao> transacoes, String rotuloParte, boolean compra) {
        if (transacoes.isEmpty()) {
            return compra
                    ? Layout.vazio("Nenhuma compra realizada", "As compras feitas no marketplace aparecem aqui.", null, null)
                    : Layout.vazio("Nenhuma venda realizada", "Quando alguém comprar das suas ofertas, a venda aparece aqui.", null, null);
        }
        StringBuilder linhas = new StringBuilder();
        double totalKwh = 0, totalValor = 0;
        for (Transacao t : transacoes) {
            totalKwh += t.getQuantidadeKwh();
            totalValor += t.getValorTotal();
            linhas.append("""
                <tr><td>%s</td><td>%s</td><td class="num">%.2f kWh</td><td class="num">R$ %.2f</td></tr>
                """.formatted(Layout.data(t.getDataTransacao()),
                    Http.escapar(compra ? t.getNomeVendedor() : t.getNomeComprador()), t.getQuantidadeKwh(), t.getValorTotal()));
        }
        return """
            <div class="tabela-envoltorio"><table>
                <thead><tr><th>Data</th><th>%s</th><th class="num">Quantidade</th><th class="num">Valor</th></tr></thead>
                <tbody>%s</tbody>
                <tfoot><tr><td colspan="2">Total</td><td class="num">%.2f kWh</td><td class="num">R$ %.2f</td></tr></tfoot>
            </table></div>
            """.formatted(rotuloParte, linhas, totalKwh, totalValor);
    }
}
