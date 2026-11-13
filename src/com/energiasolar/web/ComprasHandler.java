package com.energiasolar.web;

import com.energiasolar.model.Transacao;
import com.energiasolar.model.Usuario;
import com.energiasolar.service.RelatorioService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

/**
 * Minhas compras (GET /compras): histórico das compras do usuário no marketplace,
 * disponível para os perfis que podem comprar ({@link Usuario#podeComprar()}).
 */
public class ComprasHandler implements HttpHandler {

    private final RelatorioService relatorioService = new RelatorioService();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        Usuario usuario = Autenticacao.usuarioLogado(ex);
        if (usuario == null) { Http.redirecionar(ex, "/login"); return; }
        if (!usuario.podeComprar()) { Http.redirecionar(ex, "/vendas"); return; }

        List<Transacao> compras = relatorioService.extratoComprasDoUsuario(usuario.getId());
        double kwh = 0, pago = 0;
        StringBuilder linhas = new StringBuilder();
        for (Transacao t : compras) {
            kwh += t.getQuantidadeKwh();
            pago += t.getValorTotal();
            linhas.append("""
                <tr>
                    <td>%s</td>
                    <td><b>%s</b></td>
                    <td class="num">%.2f kWh</td>
                    <td class="num">R$ %.2f</td>
                    <td class="num">R$ %.2f</td>
                </tr>
                """.formatted(Layout.data(t.getDataTransacao()), Http.escapar(t.getNomeVendedor()), t.getQuantidadeKwh(),
                    t.getQuantidadeKwh() > 0 ? t.getValorTotal() / t.getQuantidadeKwh() : 0, t.getValorTotal()));
        }

        String tabela = compras.isEmpty()
                ? Layout.vazio("Você ainda não comprou energia",
                        "As ofertas dos geradores da sua região estão no Início.", "/marketplace", "Ver ofertas")
                : """
                  <div class="tabela-envoltorio"><table>
                      <thead><tr><th>Data</th><th>Vendedor</th><th class="num">Quantidade</th><th class="num">Preço por kWh</th><th class="num">Valor</th></tr></thead>
                      <tbody>%s</tbody>
                      <tfoot><tr><td colspan="2">Total</td><td class="num">%.2f kWh</td><td></td><td class="num">R$ %.2f</td></tr></tfoot>
                  </table></div>
                  """.formatted(linhas, kwh, pago);

        String conteudo = """
            <div class="cabecalho-pagina">
                <h2>Minhas compras</h2>
                <p>A energia que você comprou de geradores da sua região. Cada compra transfere os kWh do crédito do vendedor.</p>
            </div>
            <div class="indicadores">
                <div class="indicador destaque"><span class="valor">%.2f<small>kWh</small></span><span class="descricao">Energia comprada</span></div>
                <div class="indicador"><span class="valor">R$ %.2f</span><span class="descricao">Total pago</span></div>
                <div class="indicador"><span class="valor">%s</span><span class="descricao">Preço médio pago por kWh</span></div>
            </div>
            <section class="secao">%s</section>
            """.formatted(kwh, pago, kwh > 0 ? "R$ %.2f".formatted(pago / kwh) : "-", tabela);

        Http.enviarHtml(ex, 200, Layout.pagina("Minhas compras", usuario, "/compras", conteudo));
    }
}
