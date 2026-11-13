package com.energiasolar.web;

import com.energiasolar.model.RegistroEnergia;
import com.energiasolar.model.Usuario;
import com.energiasolar.model.UnidadeConsumidora;
import com.energiasolar.service.EnergiaService;
import com.energiasolar.service.RegraNegocioException;
import com.energiasolar.service.UnidadeService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

public class EnergiaHandler implements HttpHandler {

    private final UnidadeService unidadeService = new UnidadeService();
    private final EnergiaService energiaService = new EnergiaService();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        Usuario usuario = Autenticacao.usuarioLogado(ex);
        if (usuario == null) { Http.redirecionar(ex, "/login"); return; }

        try {
            if (ex.getRequestMethod().equals("POST")) {
                Formulario f = new Formulario(Http.parseFormBody(ex));
                int unidadeId = f.inteiro("unidadeId", "Unidade consumidora");
                unidadeService.buscarDoUsuario(unidadeId, usuario.getId());
                RegistroEnergia registro = energiaService.lancarRegistro(unidadeId,
                        f.texto("mes", "Mês de referência"),
                        f.decimal("gerada", "Energia gerada"),
                        f.decimal("consumida", "Energia consumida"));

                String msg = !usuario.podeOfertar()
                        ? "Consumo de %.2f kWh registrado para %s.".formatted(registro.getEnergiaConsumidaKwh(),
                                Layout.mes(registro.getMesReferencia()))
                        : registro.getExcedenteKwh() > 0
                        ? "Registro lançado! Excedente de %.2f kWh gerou um novo crédito de energia.".formatted(registro.getExcedenteKwh())
                        : "Registro lançado. Sem excedente neste mês (consumo igual ou maior que a geração).";
                exibirPagina(ex, usuario, msg, false);
                return;
            }
            exibirPagina(ex, usuario, null, false);
        } catch (RegraNegocioException e) {
            exibirPagina(ex, usuario, "Não foi possível lançar o registro: " + e.getMessage(), true);
        } catch (Exception e) {
            e.printStackTrace();
            exibirPagina(ex, usuario, "Ocorreu um erro inesperado ao lançar o registro. Tente novamente.", true);
        }
    }

    /**
     * Quem tem geração própria lança geração e consumo; o perfil Consumidor lança apenas o
     * consumo, e a geração é enviada como zero.
     */
    private void exibirPagina(HttpExchange ex, Usuario usuario, String mensagem, boolean erro) throws IOException {
        List<UnidadeConsumidora> unidades = unidadeService.listarDoUsuario(usuario.getId());
        boolean gera = usuario.podeOfertar();
        String tituloPagina = gera ? "Registrar energia" : "Registrar consumo";

        if (unidades.isEmpty()) {
            String conteudo = """
                <div class="cabecalho-pagina"><h2>%s</h2></div>
                %s
                """.formatted(gera ? "Registrar geração e consumo" : "Registrar consumo", Layout.vazio("Cadastre uma unidade primeiro",
                    "O lançamento mensal é feito por unidade consumidora.", "/unidades", "Cadastrar unidade"));
            Http.enviarHtml(ex, 200, Layout.pagina(tituloPagina, usuario, "/energia", conteudo));
            return;
        }

        StringBuilder opcoesUnidade = new StringBuilder();
        for (UnidadeConsumidora u : unidades) {
            opcoesUnidade.append("<option value=\"%d\">%s, %s</option>".formatted(u.getId(), Http.escapar(u.getNumeroUc()), Http.escapar(u.getEndereco())));
        }

        StringBuilder historico = new StringBuilder();
        for (UnidadeConsumidora u : unidades) {
            for (RegistroEnergia r : energiaService.historicoDaUnidade(u.getId())) {
                String colunasGeracao = gera ? """
                    <td class="num">%.2f kWh</td>
                    <td class="num">%.2f kWh</td>
                    <td class="num %s">%.2f kWh</td>
                    """.formatted(r.getEnergiaGeradaKwh(), r.getEnergiaConsumidaKwh(),
                        r.getExcedenteKwh() > 0 ? "positivo" : "", r.getExcedenteKwh())
                        : "<td class=\"num\">%.2f kWh</td>".formatted(r.getEnergiaConsumidaKwh());
                historico.append("<tr><td>%s</td><td>%s</td>%s</tr>".formatted(
                        Layout.mes(r.getMesReferencia()), Http.escapar(u.getNumeroUc()), colunasGeracao));
            }
        }

        String cabecalhoTabela = gera
                ? "<th>Mês</th><th>Unidade</th><th class=\"num\">Geração</th><th class=\"num\">Consumo</th><th class=\"num\">Excedente</th>"
                : "<th>Mês</th><th>Unidade</th><th class=\"num\">Consumo</th>";
        String tabelaHistorico = historico.isEmpty()
                ? Layout.vazio("Nenhum mês lançado ainda", gera ? "Os lançamentos aparecem aqui, com o excedente de cada mês."
                        : "O consumo de cada mês aparece aqui.", null, null)
                : """
                  <div class="tabela-envoltorio"><table>
                      <thead><tr>%s</tr></thead>
                      <tbody>%s</tbody>
                  </table></div>
                  """.formatted(cabecalhoTabela, historico);

        String camposEnergia = gera ? """
            <div class="campos-linha">
                <div>
                    <label for="gerada">Energia gerada (kWh)</label>
                    <input type="text" id="gerada" name="gerada" inputmode="decimal" placeholder="ex: 620,5" required>
                </div>
                <div>
                    <label for="consumida">Energia consumida (kWh)</label>
                    <input type="text" id="consumida" name="consumida" inputmode="decimal" placeholder="ex: 410" required>
                </div>
            </div>
            """ : """
            <input type="hidden" name="gerada" value="0">
            <label for="consumida">Energia consumida (kWh)</label>
            <input type="text" id="consumida" name="consumida" inputmode="decimal" placeholder="ex: 350" required>
            <p class="dica">Está na conta de luz, no campo de consumo do mês.</p>
            """;

        String conteudo = """
            <div class="cabecalho-pagina">
                <h2>%s</h2>
                <p>%s</p>
            </div>
            %s
            <div class="grade-2 invertida">
                <section class="caixa">
                    <h3>Lançamento mensal</h3>
                    <form method="POST" action="/energia">
                        <label for="unidadeId">Unidade consumidora</label>
                        <select id="unidadeId" name="unidadeId">%s</select>
                        <label for="mes">Mês de referência</label>
                        <input type="month" id="mes" name="mes" required>
                        %s
                        <button type="submit">Lançar registro</button>
                    </form>
                </section>
                <section>
                    <div class="secao-topo"><h3>Histórico de lançamentos</h3></div>
                    %s
                </section>
            </div>
            """.formatted(gera ? "Registrar geração e consumo" : "Registrar consumo",
                gera ? "Informe a leitura do mês. Quando a geração supera o consumo, a diferença vira crédito de energia válido por 60 meses."
                        : "Registre o consumo mensal das suas unidades para acompanhar quanto precisa comprar no marketplace.",
                Layout.alerta(erro ? "erro" : "sucesso", mensagem), opcoesUnidade, camposEnergia, tabelaHistorico);

        Http.enviarHtml(ex, 200, Layout.pagina(tituloPagina, usuario, "/energia", conteudo));
    }
}
