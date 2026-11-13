package com.energiasolar.web;

import com.energiasolar.dao.UsuarioDAO;
import com.energiasolar.model.OfertaVenda;
import com.energiasolar.model.Transacao;
import com.energiasolar.model.UnidadeConsumidora;
import com.energiasolar.model.Usuario;
import com.energiasolar.service.MarketplaceService;
import com.energiasolar.service.RegraNegocioException;
import com.energiasolar.service.UnidadeService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Página inicial do marketplace: a vitrine com as ofertas abertas dos centros de distribuição
 * das unidades do usuário, e a compra (POST /marketplace/comprar).
 *
 * Todos os perfis veem a vitrine. Quem pode comprar recebe o formulário de compra em cada
 * oferta; quem só vende vê as ofertas como referência de preço da região. As ofertas do
 * próprio usuário aparecem marcadas, com atalho para a área de Vendas.
 */
public class MarketplaceHandler implements HttpHandler {

    private final UnidadeService unidadeService = new UnidadeService();
    private final MarketplaceService marketplaceService = new MarketplaceService();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        Usuario usuario = Autenticacao.usuarioLogado(ex);
        if (usuario == null) { Http.redirecionar(ex, "/login"); return; }

        String rota = ex.getRequestURI().getPath();
        boolean post = ex.getRequestMethod().equals("POST");

        try {
            if (post && rota.equals("/marketplace/comprar")) {
                Formulario f = new Formulario(Http.parseFormBody(ex));
                Transacao t = marketplaceService.comprar(f.inteiro("ofertaId", "Oferta"), usuario.getId(),
                        f.decimal("quantidade", "Quantidade"));
                Http.redirecionar(ex, "/marketplace?msg=compra&q=" + numero(t.getQuantidadeKwh()) +
                        "&v=" + numero(t.getValorTotal()));
            } else {
                exibirVitrine(ex, usuario, mensagemDeSucesso(ex), false);
            }
        } catch (RegraNegocioException e) {
            exibirVitrine(ex, usuario, e.getMessage(), true);
        } catch (Exception e) {
            e.printStackTrace();
            exibirVitrine(ex, usuario, "Ocorreu um erro inesperado. Tente novamente.", true);
        }
    }

    private String mensagemDeSucesso(HttpExchange ex) {
        if (!"compra".equals(Http.parametro(ex, "msg"))) return null;
        return "Compra realizada: " + formatar(Http.parametro(ex, "q")) + " kWh por R$ " +
                formatar(Http.parametro(ex, "v")) + ". Ela aparece em Minhas compras.";
    }

    private void exibirVitrine(HttpExchange ex, Usuario usuario, String mensagem, boolean erro) throws IOException {
        List<UnidadeConsumidora> unidades = unidadeService.listarDoUsuario(usuario.getId());
        String ordem = Http.parametro(ex, "ordem");

        String corpo;
        if (unidades.isEmpty()) {
            corpo = Layout.vazio("Cadastre uma unidade para ver as ofertas",
                    "A vitrine mostra as ofertas do centro de distribuição que atende a sua unidade consumidora.",
                    "/unidades", "Cadastrar unidade");
        } else {
            List<OfertaVenda> ofertas = new ArrayList<>();
            Set<Integer> centros = new HashSet<>();
            for (UnidadeConsumidora u : unidades) {
                if (centros.add(u.getCentroDistribuicaoId())) {
                    ofertas.addAll(marketplaceService.vitrineDoCentro(u.getCentroDistribuicaoId()));
                }
            }
            ofertas.sort(switch (ordem == null ? "" : ordem) {
                case "quantidade" -> Comparator.comparingDouble(OfertaVenda::getQuantidadeDisponivelKwh).reversed();
                case "recentes" -> Comparator.comparingInt(OfertaVenda::getId).reversed();
                default -> Comparator.comparingDouble(OfertaVenda::getPrecoKwh);
            });
            corpo = vitrine(usuario, ofertas, ordem);
        }

        String aviso = usuario.podeComprar() ? "" : """
            <div class="nota"><span>Seu perfil vende energia: aqui você acompanha as ofertas da sua região como
            referência de preço.</span><a class="botao" href="/vendas/ofertas#nova-oferta">Anunciar crédito</a></div>
            """;

        String conteudo = """
            <section class="vitrine-topo">
                <div>
                    <h2>Energia solar da sua região</h2>
                    <p>Créditos de energia excedente ofertados por geradores do mesmo centro de distribuição.
                    A compra transfere os kWh do crédito do vendedor para você.</p>
                </div>
            </section>
            %s
            %s
            %s
            """.formatted(Layout.alerta(erro ? "erro" : "sucesso", mensagem), aviso, corpo);

        Http.enviarHtml(ex, 200, Layout.pagina("Início", usuario, "/marketplace", conteudo));
    }

    private String vitrine(Usuario usuario, List<OfertaVenda> ofertas, String ordem) {
        String topo = """
            <div class="secao-topo">
                <div class="vitrine-resumo">%s</div>
                %s
            </div>
            """.formatted(resumoVitrine(ofertas), ofertas.size() > 1 ? seletorOrdem(ordem) : "");

        if (ofertas.isEmpty()) {
            return topo + Layout.vazio("Nenhuma oferta aberta no seu centro agora",
                    "Quando um gerador da sua região publicar crédito, ele aparece aqui.", null, null);
        }

        StringBuilder cards = new StringBuilder();
        for (OfertaVenda o : ofertas) cards.append(cardOferta(usuario, o));

        return """
            %s
            <div class="ofertas">%s</div>
            <script>
                document.querySelectorAll('form[data-preco]').forEach(function (f) {
                    var campo = f.querySelector('input[name=quantidade]');
                    var total = f.querySelector('.total');
                    var preco = parseFloat(f.dataset.preco);
                    var max = parseFloat(f.dataset.max);
                    campo.addEventListener('input', function () {
                        var q = parseFloat(campo.value.replace(',', '.'));
                        if (!(q > 0)) { total.textContent = 'Informe quantos kWh deseja comprar.'; return; }
                        if (q > max) { total.textContent = 'Máximo disponível nesta oferta: ' + max.toLocaleString('pt-BR', {minimumFractionDigits: 2}) + ' kWh.'; return; }
                        total.innerHTML = 'Total da compra: <b>R$ ' + (q * preco).toLocaleString('pt-BR', {minimumFractionDigits: 2, maximumFractionDigits: 2}) + '</b>';
                    });
                });
                var ordem = document.getElementById('ordem');
                if (ordem) ordem.addEventListener('change', function () { ordem.form.submit(); });
            </script>
            """.formatted(topo, cards);
    }

    private String resumoVitrine(List<OfertaVenda> ofertas) {
        if (ofertas.isEmpty()) return "<span>Nenhuma oferta aberta no momento</span>";
        double total = 0, menor = Double.MAX_VALUE;
        for (OfertaVenda o : ofertas) {
            total += o.getQuantidadeDisponivelKwh();
            menor = Math.min(menor, o.getPrecoKwh());
        }
        return "<span><b>%d</b> %s</span><span><b>%.2f kWh</b> à venda</span><span>a partir de <b>R$ %.2f</b> por kWh</span>"
                .formatted(ofertas.size(), ofertas.size() == 1 ? "oferta aberta" : "ofertas abertas", total, menor);
    }

    private String seletorOrdem(String ordem) {
        String atual = ordem == null ? "preco" : ordem;
        StringBuilder opcoes = new StringBuilder();
        String[][] itens = {{"preco", "Menor preço"}, {"quantidade", "Mais kWh disponíveis"}, {"recentes", "Mais recentes"}};
        for (String[] item : itens) {
            opcoes.append("<option value=\"%s\"%s>%s</option>".formatted(item[0], item[0].equals(atual) ? " selected" : "", item[1]));
        }
        return """
            <form method="GET" action="/marketplace" class="ordenar">
                <label for="ordem">Ordenar por</label>
                <select id="ordem" name="ordem">%s</select>
                <noscript><button type="submit" class="botao-secundario pequeno">Aplicar</button></noscript>
            </form>
            """.formatted(opcoes);
    }

    private String cardOferta(Usuario usuario, OfertaVenda o) {
        boolean minha = o.getVendedorId() == usuario.getId();
        Usuario vendedor = usuarioDAO.buscarPorId(o.getVendedorId()).orElse(null);
        String selo = minha ? "<span class=\"selo selo-minha\">Sua oferta</span>"
                : vendedor == null ? "" : Layout.selo(vendedor.getTipo(), vendedor.rotuloPerfil());
        double restante = o.getQuantidadeKwh() > 0 ? 100.0 * o.getQuantidadeDisponivelKwh() / o.getQuantidadeKwh() : 0;

        String rodape;
        if (minha) {
            rodape = """
                <div class="oferta-rodape"><a class="botao botao-secundario" href="/vendas/ofertas">Gerenciar em Vendas</a></div>
                """;
        } else if (usuario.podeComprar()) {
            rodape = """
                <form method="POST" action="/marketplace/comprar" class="oferta-compra" data-preco="%s" data-max="%s">
                    <input type="hidden" name="ofertaId" value="%d">
                    <label for="qtd-%d" class="visualmente-oculto">Quantidade em kWh</label>
                    <div class="linha-compra">
                        <input type="text" id="qtd-%d" name="quantidade" inputmode="decimal" placeholder="Quantos kWh?" required>
                        <button type="submit">Comprar</button>
                    </div>
                    <p class="total" aria-live="polite">Informe quantos kWh deseja comprar.</p>
                </form>
                """.formatted(numero(o.getPrecoKwh()), numero(o.getQuantidadeDisponivelKwh()), o.getId(), o.getId(), o.getId());
        } else {
            rodape = "<div class=\"oferta-rodape\"><p class=\"dica\">Compra disponível para os perfis Consumidor e Ambos.</p></div>";
        }

        return """
            <article class="oferta%s">
                <div class="oferta-topo">
                    <div class="oferta-vendedor"><strong>%s</strong>%s</div>
                    <p class="oferta-centro">%s</p>
                </div>
                <div class="medidor">
                    <div class="medidor-leitura"><b>%.2f</b><span>kWh disponíveis</span></div>
                    <div class="medidor-barra"><i style="width:%.0f%%"></i></div>
                    <p class="medidor-legenda">%.0f%% dos %.2f kWh ofertados ainda à venda</p>
                </div>
                <div class="oferta-preco"><b>R$ %.2f</b><span>por kWh</span></div>
                %s
            </article>
            """.formatted(minha ? " oferta-minha" : "", Http.escapar(o.getNomeVendedor()), selo, Http.escapar(o.getNomeCentro()),
                o.getQuantidadeDisponivelKwh(), restante, restante, o.getQuantidadeKwh(), o.getPrecoKwh(), rodape);
    }

    private static String numero(double valor) {
        return String.format(Locale.ROOT, "%.2f", valor);
    }

    private static String formatar(String valor) {
        try {
            return String.format(new Locale("pt", "BR"), "%.2f", Double.parseDouble(valor));
        } catch (Exception e) {
            return "-";
        }
    }
}
