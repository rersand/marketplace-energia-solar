package com.energiasolar.web;

import com.energiasolar.dao.CentroDistribuicaoDAO;
import com.energiasolar.model.CentroDistribuicao;
import com.energiasolar.model.UnidadeConsumidora;
import com.energiasolar.model.Usuario;
import com.energiasolar.service.RegraNegocioException;
import com.energiasolar.service.UnidadeService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * CRUD de unidades consumidoras.
 *
 * Rotas: GET /unidades (listagem e cadastro), POST /unidades (criar),
 * GET /unidades/editar?id= (formulário de edição), POST /unidades/editar (atualizar)
 * e POST /unidades/excluir (excluir).
 */
public class UnidadeHandler implements HttpHandler {

    private final UnidadeService unidadeService = new UnidadeService();
    private final CentroDistribuicaoDAO centroDAO = new CentroDistribuicaoDAO();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        Usuario usuario = Autenticacao.usuarioLogado(ex);
        if (usuario == null) { Http.redirecionar(ex, "/login"); return; }

        String rota = ex.getRequestURI().getPath();
        boolean post = ex.getRequestMethod().equals("POST");

        try {
            if (rota.equals("/unidades") && post) {
                cadastrar(ex, usuario);
            } else if (rota.equals("/unidades/editar") && post) {
                atualizar(ex, usuario);
            } else if (rota.equals("/unidades/editar")) {
                UnidadeConsumidora u = unidadeService.buscarDoUsuario(Http.parametroInteiro(ex, "id"), usuario.getId());
                exibirEdicao(ex, usuario, u, null);
            } else if (rota.equals("/unidades/excluir") && post) {
                excluir(ex, usuario);
            } else {
                exibirListagem(ex, usuario, null, mensagemDeSucesso(ex));
            }
        } catch (RegraNegocioException e) {
            exibirListagem(ex, usuario, e.getMessage(), null);
        } catch (Exception e) {
            e.printStackTrace();
            exibirListagem(ex, usuario, "Ocorreu um erro inesperado. Tente novamente.", null);
        }
    }

    private void cadastrar(HttpExchange ex, Usuario usuario) throws IOException {
        Formulario f = new Formulario(Http.parseFormBody(ex));
        try {
            UnidadeConsumidora u = unidadeService.cadastrar(usuario.getId(),
                    f.inteiro("centroId", "Centro de distribuição"),
                    f.texto("numeroUc", "Número da UC"),
                    f.texto("endereco", "Endereço"),
                    f.decimalOuZero("capacidade", "Capacidade instalada"));
            Http.redirecionar(ex, "/unidades?msg=cadastrada&uc=" + codificar(u.getNumeroUc()));
        } catch (RegraNegocioException e) {
            exibirListagem(ex, usuario, "Não foi possível cadastrar: " + e.getMessage(), null);
        }
    }

    private void atualizar(HttpExchange ex, Usuario usuario) throws IOException {
        Formulario f = new Formulario(Http.parseFormBody(ex));
        int id = f.inteiro("id", "Unidade");
        try {
            UnidadeConsumidora u = unidadeService.atualizar(id, usuario.getId(),
                    f.inteiro("centroId", "Centro de distribuição"),
                    f.texto("numeroUc", "Número da UC"),
                    f.texto("endereco", "Endereço"),
                    f.decimalOuZero("capacidade", "Capacidade instalada"));
            Http.redirecionar(ex, "/unidades?msg=atualizada&uc=" + codificar(u.getNumeroUc()));
        } catch (RegraNegocioException e) {
            UnidadeConsumidora atual = unidadeService.buscarDoUsuario(id, usuario.getId());
            exibirEdicao(ex, usuario, atual, e.getMessage());
        }
    }

    private void excluir(HttpExchange ex, Usuario usuario) throws IOException {
        Formulario f = new Formulario(Http.parseFormBody(ex));
        UnidadeService.ResultadoExclusao r = unidadeService.excluir(f.inteiro("id", "Unidade"), usuario.getId());
        Http.redirecionar(ex, "/unidades?msg=excluida&uc=" + codificar(r.numeroUc()) +
                "&r=" + r.registrosRemovidos() + "&c=" + r.creditosRemovidos());
    }

    private String mensagemDeSucesso(HttpExchange ex) {
        String msg = Http.parametro(ex, "msg");
        String uc = Http.parametro(ex, "uc");
        if (msg == null || uc == null) return null;
        return switch (msg) {
            case "cadastrada" -> "Unidade " + uc + " cadastrada com sucesso.";
            case "atualizada" -> "Unidade " + uc + " atualizada com sucesso.";
            case "excluida" -> "Unidade " + uc + " excluída, junto com " +
                    Math.max(0, Http.parametroInteiro(ex, "r")) + " lançamento(s) de energia e " +
                    Math.max(0, Http.parametroInteiro(ex, "c")) + " crédito(s) gerados por ela.";
            default -> null;
        };
    }

    private void exibirListagem(HttpExchange ex, Usuario usuario, String erro, String sucesso) throws IOException {
        List<UnidadeConsumidora> unidades = unidadeService.listarDoUsuario(usuario.getId());

        StringBuilder linhas = new StringBuilder();
        for (UnidadeConsumidora u : unidades) {
            linhas.append("""
                <tr>
                    <td><b>%s</b><span class="secundario">%s</span></td>
                    <td>%s</td>
                    <td class="num">%.2f kWp</td>
                    <td><div class="acoes">
                        <a class="botao botao-secundario pequeno" href="/unidades/editar?id=%d">Editar</a>
                        <form method="POST" action="/unidades/excluir"
                              onsubmit="return confirm('Excluir esta unidade? Os lançamentos de energia e os créditos dela também serão removidos.');">
                            <input type="hidden" name="id" value="%d">
                            <button type="submit" class="botao-perigo pequeno">Excluir</button>
                        </form>
                    </div></td>
                </tr>
                """.formatted(Http.escapar(u.getNumeroUc()), Http.escapar(u.getEndereco()),
                    Http.escapar(u.getNomeCentro()), u.getCapacidadeGeracaoKwp(),
                    u.getId(), u.getId()));
        }

        String tabela = unidades.isEmpty()
                ? Layout.vazio("Nenhuma unidade cadastrada ainda",
                        "Use o formulário ao lado com o número da UC que aparece na sua conta de luz.", null, null)
                : """
                  <div class="tabela-envoltorio"><table>
                      <thead><tr><th>Unidade</th><th>Centro de distribuição</th><th class="num">Capacidade</th><th></th></tr></thead>
                      <tbody>%s</tbody>
                  </table></div>
                  <p class="dica">Uma unidade com ofertas publicadas no marketplace só pode ser excluída depois que essas ofertas forem excluídas.
                  Se alguma oferta já teve vendas, a unidade fica preservada como histórico financeiro.</p>
                  """.formatted(linhas);

        String conteudo = """
            <div class="cabecalho-pagina">
                <h2>Minhas unidades consumidoras</h2>
                <p>Cada unidade é um ponto de consumo da distribuidora. É nela que a geração é medida e o crédito é registrado.</p>
            </div>
            %s
            %s
            <div class="grade-2">
                <section>%s</section>
                <section class="caixa">
                    <h3>Cadastrar unidade</h3>
                    <form method="POST" action="/unidades">
                        %s
                        <button type="submit">Cadastrar unidade</button>
                    </form>
                </section>
            </div>
            """.formatted(Layout.alerta("sucesso", sucesso), Layout.alerta("erro", erro), tabela,
                camposUnidade(null));

        Http.enviarHtml(ex, 200, Layout.pagina("Unidades consumidoras", usuario, "/unidades", conteudo));
    }

    private void exibirEdicao(HttpExchange ex, Usuario usuario, UnidadeConsumidora u, String erro) throws IOException {
        String conteudo = """
            <div class="cabecalho-pagina">
                <h2>Editar unidade %s</h2>
                <p>A troca de centro de distribuição só é permitida quando a unidade não tem ofertas abertas no marketplace.</p>
            </div>
            %s
            <section class="caixa estreita">
                <form method="POST" action="/unidades/editar">
                    <input type="hidden" name="id" value="%d">
                    %s
                    <div class="acoes-form">
                        <button type="submit">Salvar alterações</button>
                        <a class="botao botao-secundario" href="/unidades">Voltar sem salvar</a>
                    </div>
                </form>
            </section>
            """.formatted(Http.escapar(u.getNumeroUc()), Layout.alerta("erro", erro), u.getId(), camposUnidade(u));

        Http.enviarHtml(ex, 200, Layout.pagina("Editar unidade", usuario, "/unidades", conteudo));
    }

    private String camposUnidade(UnidadeConsumidora u) {
        StringBuilder opcoes = new StringBuilder();
        for (CentroDistribuicao c : centroDAO.listarTodos()) {
            boolean selecionado = u != null && u.getCentroDistribuicaoId() == c.getId();
            opcoes.append("<option value=\"%d\"%s>%s (%s/%s)</option>".formatted(c.getId(),
                    selecionado ? " selected" : "", Http.escapar(c.getNome()),
                    Http.escapar(c.getCidade()), Http.escapar(c.getUf())));
        }
        return """
            <label for="numeroUc">Número da UC</label>
            <input type="text" id="numeroUc" name="numeroUc" value="%s" placeholder="ex: UC-1001" required minlength="3" maxlength="30">
            <label for="endereco">Endereço</label>
            <input type="text" id="endereco" name="endereco" value="%s" placeholder="Rua, número, cidade" required minlength="5" maxlength="200">
            <label for="centroId">Centro de distribuição</label>
            <select id="centroId" name="centroId">%s</select>
            <label for="capacidade">Capacidade de geração instalada (kWp)</label>
            <input type="text" id="capacidade" name="capacidade" value="%s" inputmode="decimal" placeholder="ex: 5,4">
            <p class="dica">Deixe 0 se a unidade não tiver painéis solares.</p>
            """.formatted(
                u == null ? "" : Http.escapar(u.getNumeroUc()),
                u == null ? "" : Http.escapar(u.getEndereco()),
                opcoes,
                u == null ? "" : String.valueOf(u.getCapacidadeGeracaoKwp()).replace(".", ","));
    }

    private static String codificar(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }
}
