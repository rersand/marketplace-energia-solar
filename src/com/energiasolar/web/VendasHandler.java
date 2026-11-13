package com.energiasolar.web;

import com.energiasolar.dao.TransacaoDAO;
import com.energiasolar.model.CreditoEnergia;
import com.energiasolar.model.OfertaVenda;
import com.energiasolar.model.RegistroEnergia;
import com.energiasolar.model.Transacao;
import com.energiasolar.model.UnidadeConsumidora;
import com.energiasolar.model.Usuario;
import com.energiasolar.service.EnergiaService;
import com.energiasolar.service.MarketplaceService;
import com.energiasolar.service.RegraNegocioException;
import com.energiasolar.service.RelatorioService;
import com.energiasolar.service.UnidadeService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Área de Vendas, exclusiva dos perfis que podem ofertar ({@link Usuario#podeOfertar()}).
 *
 * Páginas: GET /vendas (resumo), GET /vendas/ofertas (nova oferta e CRUD das ofertas) e
 * GET /vendas/estoque (créditos de energia disponíveis para anunciar). Ações via POST em
 * /vendas/ofertar, /vendas/editar, /vendas/cancelar e /vendas/excluir, sempre terminando em
 * redirecionamento (padrão Post/Redirect/Get).
 */
public class VendasHandler implements HttpHandler {

    private final UnidadeService unidadeService = new UnidadeService();
    private final EnergiaService energiaService = new EnergiaService();
    private final MarketplaceService marketplaceService = new MarketplaceService();
    private final RelatorioService relatorioService = new RelatorioService();
    private final TransacaoDAO transacaoDAO = new TransacaoDAO();

    private static final String OFERTAS = "/vendas/ofertas";

    @Override
    public void handle(HttpExchange ex) throws IOException {
        Usuario usuario = Autenticacao.usuarioLogado(ex);
        if (usuario == null) { Http.redirecionar(ex, "/login"); return; }
        if (!usuario.podeOfertar()) { Http.redirecionar(ex, "/marketplace"); return; }

        String rota = ex.getRequestURI().getPath();
        boolean post = ex.getRequestMethod().equals("POST");

        try {
            if (post && rota.equals("/vendas/ofertar")) {
                Formulario f = new Formulario(Http.parseFormBody(ex));
                marketplaceService.publicarOferta(f.inteiro("creditoId", "Crédito"), usuario.getId(),
                        f.decimal("quantidade", "Quantidade"), f.decimal("preco", "Preço por kWh"));
                Http.redirecionar(ex, OFERTAS + "?msg=publicada");
            } else if (post && rota.equals("/vendas/editar")) {
                Formulario f = new Formulario(Http.parseFormBody(ex));
                OfertaVenda o = marketplaceService.editarPreco(f.inteiro("ofertaId", "Oferta"), usuario.getId(),
                        f.decimal("preco", "Novo preço por kWh"));
                Http.redirecionar(ex, OFERTAS + "?msg=preco&v=" + numero(o.getPrecoKwh()));
            } else if (post && rota.equals("/vendas/cancelar")) {
                Formulario f = new Formulario(Http.parseFormBody(ex));
                double devolvido = marketplaceService.cancelarOferta(f.inteiro("ofertaId", "Oferta"), usuario.getId());
                Http.redirecionar(ex, OFERTAS + "?msg=cancelada&q=" + numero(devolvido));
            } else if (post && rota.equals("/vendas/excluir")) {
                Formulario f = new Formulario(Http.parseFormBody(ex));
                double devolvido = marketplaceService.excluirOferta(f.inteiro("ofertaId", "Oferta"), usuario.getId());
                Http.redirecionar(ex, OFERTAS + "?msg=excluida&q=" + numero(devolvido));
            } else if (rota.equals(OFERTAS)) {
                exibirOfertas(ex, usuario, mensagemDeSucesso(ex), false);
            } else if (rota.equals("/vendas/estoque")) {
                exibirEstoque(ex, usuario);
            } else {
                exibirResumo(ex, usuario);
            }
        } catch (RegraNegocioException e) {
            exibirOfertas(ex, usuario, e.getMessage(), true);
        } catch (Exception e) {
            e.printStackTrace();
            exibirOfertas(ex, usuario, "Ocorreu um erro inesperado. Tente novamente.", true);
        }
    }

    private String mensagemDeSucesso(HttpExchange ex) {
        String msg = Http.parametro(ex, "msg");
        if (msg == null) return null;
        String q = formatar(Http.parametro(ex, "q"));
        String v = formatar(Http.parametro(ex, "v"));
        return switch (msg) {
            case "publicada" -> "Oferta publicada com sucesso. Ela já aparece na vitrine do seu centro de distribuição.";
            case "preco" -> "Preço da oferta atualizado para R$ " + v + " por kWh.";
            case "cancelada" -> "Oferta cancelada. " + q + " kWh não vendidos voltaram ao saldo do crédito de origem.";
            case "excluida" -> "Oferta excluída. " + q + " kWh voltaram ao saldo do crédito de origem.";
            default -> null;
        };
    }

    private void exibirResumo(HttpExchange ex, Usuario usuario) throws IOException {
        double estoque = 0;
        for (UnidadeConsumidora u : unidadeService.listarDoUsuario(usuario.getId())) {
            estoque += energiaService.saldoTotalDisponivel(u.getId());
        }

        List<OfertaVenda> ofertas = marketplaceService.minhasOfertas(usuario.getId());
        int abertas = 0;
        double aVenda = 0;
        StringBuilder listaAbertas = new StringBuilder();
        for (OfertaVenda o : ofertas) {
            if (!"ABERTA".equals(o.getStatus())) continue;
            abertas++;
            aVenda += o.getQuantidadeDisponivelKwh();
            listaAbertas.append("""
                <li><div><span class="uc">Oferta #%d</span><span class="validade">%.2f de %.2f kWh à venda</span></div>
                <span class="saldo">R$ %.2f/kWh</span></li>
                """.formatted(o.getId(), o.getQuantidadeDisponivelKwh(), o.getQuantidadeKwh(), o.getPrecoKwh()));
        }

        List<Transacao> vendas = relatorioService.extratoVendasDoUsuario(usuario.getId());
        double vendido = 0, faturado = 0;
        StringBuilder linhas = new StringBuilder();
        for (int i = 0; i < vendas.size(); i++) {
            Transacao t = vendas.get(i);
            vendido += t.getQuantidadeKwh();
            faturado += t.getValorTotal();
            if (i < 5) {
                linhas.append("<tr><td>%s</td><td>%s</td><td class=\"num\">%.2f kWh</td><td class=\"num\">R$ %.2f</td></tr>"
                        .formatted(Layout.data(t.getDataTransacao()), Http.escapar(t.getNomeComprador()),
                                t.getQuantidadeKwh(), t.getValorTotal()));
            }
        }

        String ultimasVendas = vendas.isEmpty()
                ? Layout.vazio("Nenhuma venda ainda", "Quando alguém comprar das suas ofertas, a venda aparece aqui.", null, null)
                : """
                  <div class="tabela-envoltorio"><table>
                      <thead><tr><th>Data</th><th>Comprador</th><th class="num">Quantidade</th><th class="num">Valor</th></tr></thead>
                      <tbody>%s</tbody>
                  </table></div>
                  """.formatted(linhas);

        String ofertasAbertas = abertas == 0
                ? Layout.vazio("Nenhuma oferta aberta", "Anuncie um crédito do seu estoque.", OFERTAS + "#nova-oferta", "Anunciar crédito")
                : "<ul class=\"lista-creditos\">" + listaAbertas + "</ul>";

        String conteudo = """
            <div class="cabecalho-pagina">
                <h2>Resumo de vendas</h2>
                <p>O que você tem em estoque, o que está anunciado e o que já vendeu.</p>
            </div>
            <div class="indicadores">
                %s%s%s%s
            </div>
            <div class="grade-2 lado secao">
                <section>
                    <div class="secao-topo"><h3>Últimas vendas</h3><a href="/relatorios">Ver relatório completo</a></div>
                    %s
                </section>
                <section>
                    <div class="secao-topo"><h3>Ofertas abertas</h3><a href="%s">Gerenciar</a></div>
                    %s
                </section>
            </div>
            """.formatted(
                indicador(true, "%.2f<small>kWh</small>".formatted(estoque), "Em estoque para anunciar"),
                indicador(false, "%.2f<small>kWh</small>".formatted(aVenda), "À venda em %d %s".formatted(abertas, abertas == 1 ? "oferta" : "ofertas")),
                indicador(false, "%.2f<small>kWh</small>".formatted(vendido), "Vendidos em %d %s".formatted(vendas.size(), vendas.size() == 1 ? "venda" : "vendas")),
                indicador(false, "R$ %.2f".formatted(faturado), "Faturamento das vendas"),
                ultimasVendas, OFERTAS, ofertasAbertas);

        Http.enviarHtml(ex, 200, Layout.pagina("Resumo de vendas", usuario, "/vendas", conteudo));
    }

    private void exibirOfertas(HttpExchange ex, Usuario usuario, String mensagem, boolean erro) throws IOException {
        int creditoEscolhido = Http.parametroInteiro(ex, "credito");
        StringBuilder creditos = new StringBuilder();
        StringBuilder opcoes = new StringBuilder();
        for (UnidadeConsumidora u : unidadeService.listarDoUsuario(usuario.getId())) {
            for (CreditoEnergia c : energiaService.creditosDisponiveis(u.getId())) {
                creditos.append("""
                    <li><div><span class="uc">%s</span><span class="validade">Crédito #%d, válido até %s</span></div>
                    <span class="saldo">%.2f kWh</span></li>
                    """.formatted(Http.escapar(u.getNumeroUc()), c.getId(), Layout.data(c.getDataValidade()),
                        c.getQuantidadeDisponivelKwh()));
                opcoes.append("<option value=\"%d\"%s>%s, crédito #%d (%.2f kWh)</option>".formatted(
                        c.getId(), c.getId() == creditoEscolhido ? " selected" : "", Http.escapar(u.getNumeroUc()),
                        c.getId(), c.getQuantidadeDisponivelKwh()));
            }
        }

        String publicar = opcoes.isEmpty()
                ? "<h3>Nova oferta</h3>" + Layout.vazio("Sem crédito em estoque",
                        "Lance um mês em que a geração superou o consumo para gerar crédito.", "/energia", "Registrar energia")
                : """
                  <h3>Nova oferta</h3>
                  <form method="POST" action="/vendas/ofertar">
                      <label for="creditoId">Crédito a ofertar</label>
                      <select id="creditoId" name="creditoId">%s</select>
                      <p class="dica">A oferta aparece para os consumidores do centro de distribuição da unidade geradora.</p>
                      <div class="campos-linha">
                          <div>
                              <label for="quantidade">Quantidade (kWh)</label>
                              <input type="text" id="quantidade" name="quantidade" inputmode="decimal" placeholder="ex: 50" required>
                          </div>
                          <div>
                              <label for="preco">Preço por kWh (R$)</label>
                              <input type="text" id="preco" name="preco" inputmode="decimal" placeholder="ex: 0,65" required>
                          </div>
                      </div>
                      <button type="submit">Publicar oferta</button>
                  </form>
                  """.formatted(opcoes);

        String saldo = creditos.isEmpty() ? "<p class=\"dica\">Nenhum crédito com saldo no momento.</p>"
                : "<ul class=\"lista-creditos\">" + creditos + "</ul>";

        String conteudo = """
            <div class="cabecalho-pagina">
                <h2>Minhas ofertas</h2>
                <p>Anuncie o crédito que sobrou da sua geração e acompanhe as suas ofertas. Elas aparecem para os
                consumidores do mesmo centro de distribuição da unidade geradora.</p>
            </div>
            %s
            <div class="grade-2 lado" id="nova-oferta">
                <section class="caixa">%s</section>
                <section class="caixa"><h3>Em estoque</h3><p class="apoio">Ao publicar, os kWh saem do estoque e ficam reservados na oferta.</p>%s
                    <p class="dica"><a href="/vendas/estoque">Ver estoque completo</a></p></section>
            </div>
            %s
            """.formatted(Layout.alerta(erro ? "erro" : "sucesso", mensagem), publicar, saldo, tabelaOfertas(usuario));

        Http.enviarHtml(ex, 200, Layout.pagina("Minhas ofertas", usuario, OFERTAS, conteudo));
    }

    private String tabelaOfertas(Usuario usuario) {
        List<OfertaVenda> ofertas = marketplaceService.minhasOfertas(usuario.getId());
        String topo = "<div class=\"secao-topo\"><div><h3>Ofertas publicadas</h3><p>Edite o preço, cancele ou exclua. " +
                "Ofertas com vendas não podem ser excluídas, porque as transações são o histórico financeiro " +
                "de compradores e vendedores: para tirá-las do marketplace, use Cancelar.</p></div></div>";
        if (ofertas.isEmpty()) {
            return "<section class=\"secao\">" + topo + Layout.vazio("Você ainda não publicou nenhuma oferta",
                    "Use o formulário Nova oferta para colocar seu crédito à venda.", null, null) + "</section>";
        }

        StringBuilder linhas = new StringBuilder();
        for (OfertaVenda o : ofertas) {
            boolean aberta = "ABERTA".equals(o.getStatus());
            String precoOuEdicao = aberta ? """
                <form method="POST" action="/vendas/editar" class="form-inline">
                    <input type="hidden" name="ofertaId" value="%d">
                    <input type="text" name="preco" value="%s" inputmode="decimal" aria-label="Novo preço por kWh" required>
                    <button type="submit" class="botao-secundario pequeno">Salvar</button>
                </form>
                """.formatted(o.getId(), numero(o.getPrecoKwh()).replace(".", ","))
                    : "R$ %.2f".formatted(o.getPrecoKwh());

            String cancelar = aberta ? """
                <form method="POST" action="/vendas/cancelar"
                      onsubmit="return confirm('Cancelar esta oferta? O saldo não vendido volta ao crédito de origem.');">
                    <input type="hidden" name="ofertaId" value="%d">
                    <button type="submit" class="botao-secundario pequeno">Cancelar</button>
                </form>
                """.formatted(o.getId()) : "";

            String excluir = """
                <form method="POST" action="/vendas/excluir"
                      onsubmit="return confirm('Excluir definitivamente esta oferta?');">
                    <input type="hidden" name="ofertaId" value="%d">
                    <button type="submit" class="botao-perigo pequeno">Excluir</button>
                </form>
                """.formatted(o.getId());

            linhas.append("""
                <tr>
                    <td><b>Oferta #%d</b><span class="secundario">%s</span></td>
                    <td><span class="status status-%s">%s</span></td>
                    <td class="num">%.2f kWh<span class="secundario">de %.2f ofertados</span></td>
                    <td class="num">%.2f kWh</td>
                    <td>%s</td>
                    <td><div class="acoes">%s%s</div></td>
                </tr>
                """.formatted(o.getId(), Http.escapar(o.getNomeCentro()), o.getStatus().toLowerCase(Locale.ROOT),
                    rotuloStatusOferta(o.getStatus()), o.getQuantidadeDisponivelKwh(), o.getQuantidadeKwh(),
                    transacaoDAO.totalVendidoPorOferta(o.getId()), precoOuEdicao, cancelar, excluir));
        }

        return """
            <section class="secao">
                %s
                <div class="tabela-envoltorio"><table>
                    <thead><tr><th>Oferta</th><th>Situação</th><th class="num">Disponível</th><th class="num">Vendido</th><th>Preço por kWh</th><th></th></tr></thead>
                    <tbody>%s</tbody>
                </table></div>
            </section>
            """.formatted(topo, linhas);
    }

    private void exibirEstoque(HttpExchange ex, Usuario usuario) throws IOException {
        double emEstoque = 0, reservado = 0;
        int ativos = 0;
        StringBuilder linhas = new StringBuilder();

        for (UnidadeConsumidora u : unidadeService.listarDoUsuario(usuario.getId())) {
            Map<Integer, String> mesDoRegistro = new HashMap<>();
            for (RegistroEnergia r : energiaService.historicoDaUnidade(u.getId())) mesDoRegistro.put(r.getId(), r.getMesReferencia());

            for (CreditoEnergia c : energiaService.historicoCreditos(u.getId())) {
                boolean anunciavel = c.getQuantidadeDisponivelKwh() > 0
                        && ("DISPONIVEL".equals(c.getStatus()) || "PARCIAL".equals(c.getStatus()));
                if (anunciavel) { ativos++; emEstoque += c.getQuantidadeDisponivelKwh(); }
                String mes = mesDoRegistro.get(c.getRegistroId());
                linhas.append("""
                    <tr>
                        <td><b>Crédito #%d</b><span class="secundario">%s</span></td>
                        <td>%s</td>
                        <td class="num">%.2f kWh</td>
                        <td class="num%s">%.2f kWh</td>
                        <td>%s</td>
                        <td><span class="status %s">%s</span></td>
                        <td><div class="acoes">%s</div></td>
                    </tr>
                    """.formatted(c.getId(), Http.escapar(u.getNumeroUc()), mes == null ? "-" : Layout.mes(mes),
                        c.getQuantidadeKwh(), anunciavel ? " positivo" : "", c.getQuantidadeDisponivelKwh(),
                        Layout.data(c.getDataValidade()), classeStatusCredito(c.getStatus()), rotuloStatusCredito(c.getStatus()),
                        anunciavel ? "<a class=\"botao pequeno\" href=\"" + OFERTAS + "?credito=" + c.getId() + "#nova-oferta\">Anunciar</a>" : ""));
            }
        }
        for (OfertaVenda o : marketplaceService.minhasOfertas(usuario.getId())) {
            if ("ABERTA".equals(o.getStatus())) reservado += o.getQuantidadeDisponivelKwh();
        }

        String tabela = linhas.isEmpty()
                ? Layout.vazio("Nenhum crédito gerado ainda",
                        "Cada mês em que a geração supera o consumo gera um crédito válido por 60 meses.", "/energia", "Registrar energia")
                : """
                  <div class="tabela-envoltorio"><table>
                      <thead><tr><th>Crédito</th><th>Origem</th><th class="num">Gerado</th><th class="num">Em estoque</th><th>Validade</th><th>Situação</th><th></th></tr></thead>
                      <tbody>%s</tbody>
                  </table></div>
                  """.formatted(linhas);

        String conteudo = """
            <div class="cabecalho-pagina">
                <h2>Estoque de créditos</h2>
                <p>Cada mês com excedente vira um crédito válido por 60 meses. O que está em estoque pode ser anunciado;
                o que já está numa oferta aberta fica reservado até ser vendido ou a oferta ser cancelada.</p>
            </div>
            <div class="indicadores">%s%s%s</div>
            <section class="secao">%s</section>
            """.formatted(
                indicador(true, "%.2f<small>kWh</small>".formatted(emEstoque), "Em estoque para anunciar"),
                indicador(false, "%.2f<small>kWh</small>".formatted(reservado), "Reservados em ofertas abertas"),
                indicador(false, String.valueOf(ativos), ativos == 1 ? "Crédito com saldo" : "Créditos com saldo"),
                tabela);

        Http.enviarHtml(ex, 200, Layout.pagina("Estoque de créditos", usuario, "/vendas/estoque", conteudo));
    }

    private static String indicador(boolean destaque, String valor, String descricao) {
        return "<div class=\"indicador%s\"><span class=\"valor\">%s</span><span class=\"descricao\">%s</span></div>"
                .formatted(destaque ? " destaque" : "", valor, descricao);
    }

    private static String rotuloStatusOferta(String status) {
        return switch (status) {
            case "ABERTA" -> "Aberta";
            case "FECHADA" -> "Vendida";
            case "CANCELADA" -> "Cancelada";
            default -> status;
        };
    }

    private static String rotuloStatusCredito(String status) {
        return switch (status) {
            case "DISPONIVEL" -> "Disponível";
            case "PARCIAL" -> "Parcial";
            case "ESGOTADO" -> "Esgotado";
            case "EXPIRADO" -> "Expirado";
            default -> status;
        };
    }

    private static String classeStatusCredito(String status) {
        return switch (status) {
            case "DISPONIVEL" -> "status-aberta";
            case "PARCIAL" -> "status-parcial";
            case "ESGOTADO" -> "status-fechada";
            default -> "status-cancelada";
        };
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
