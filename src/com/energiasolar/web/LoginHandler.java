package com.energiasolar.web;

import com.energiasolar.model.Usuario;
import com.energiasolar.service.AutenticacaoService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class LoginHandler implements HttpHandler {

    private final AutenticacaoService autenticacaoService = new AutenticacaoService();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String metodo = ex.getRequestMethod();

        try {
            if (path.equals("/login") && metodo.equals("GET")) {
                exibirLogin(ex, null);
            } else if (path.equals("/login") && metodo.equals("POST")) {
                processarLogin(ex);
            } else if (path.equals("/cadastro") && metodo.equals("GET")) {
                exibirCadastro(ex, null);
            } else if (path.equals("/cadastro") && metodo.equals("POST")) {
                processarCadastro(ex);
            } else if (path.equals("/logout")) {
                Map<String, String> cookies = Http.parseCookies(ex);
                SessaoManager.encerrarSessao(cookies.get("SESSIONID"));
                Http.limparCookieSessao(ex);
                Http.redirecionar(ex, "/login");
            } else {
                Http.enviarHtml(ex, 404, "<h1>404 - Página não encontrada</h1>");
            }
        } catch (Exception e) {
            Http.enviarHtml(ex, 500, "<h1>Erro interno</h1><p>" + e.getMessage() + "</p>");
        }
    }

    private void exibirLogin(HttpExchange ex, String erro) throws IOException {
        String conteudo = """
            <h2>Entrar</h2>
            <p>Acesse com o e-mail cadastrado.</p>
            %s
            <form method="POST" action="/login" id="form-login">
                <label for="email">E-mail</label>
                <input type="email" id="email" name="email" autocomplete="username" required>
                <label for="senha">Senha</label>
                <input type="password" id="senha" name="senha" autocomplete="current-password" required>
                <button type="submit">Entrar</button>
            </form>
            <p class="troca">Ainda não tem conta? <a href="/cadastro">Criar conta</a></p>
            <div class="contas-teste">
                <p>Contas de demonstração (senha 123456). Clique para preencher:</p>
                <div class="opcoes">
                    <button type="button" class="conta-gerador" data-email="ana@exemplo.com">Ana<span>Gerador</span></button>
                    <button type="button" class="conta-consumidor" data-email="joao@exemplo.com">João<span>Consumidor</span></button>
                    <button type="button" class="conta-ambos" data-email="carla@exemplo.com">Carla<span>Ambos</span></button>
                </div>
            </div>
            <script>
                document.querySelectorAll('[data-email]').forEach(function (b) {
                    b.addEventListener('click', function () {
                        document.getElementById('email').value = b.dataset.email;
                        document.getElementById('senha').value = '123456';
                        document.getElementById('senha').focus();
                    });
                });
            </script>
            """.formatted(Layout.alerta("erro", erro));
        Http.enviarHtml(ex, 200, Layout.paginaAcesso("Entrar", conteudo));
    }

    private void processarLogin(HttpExchange ex) throws IOException {
        Map<String, String> form = Http.parseFormBody(ex);
        Optional<Usuario> usuario = autenticacaoService.autenticar(form.get("email"), form.get("senha"));
        if (usuario.isPresent()) {
            String sessionId = SessaoManager.criarSessao(usuario.get().getId());
            Http.setCookieSessao(ex, sessionId);
            Http.redirecionar(ex, "/marketplace");
        } else {
            exibirLogin(ex, "E-mail ou senha inválidos.");
        }
    }

    private void exibirCadastro(HttpExchange ex, String erro) throws IOException {
        String conteudo = """
            <h2>Criar conta</h2>
            <p>Escolha o perfil conforme a sua participação no marketplace.</p>
            %s
            <form method="POST" action="/cadastro">
                <label for="nome">Nome</label>
                <input type="text" id="nome" name="nome" autocomplete="name" required>
                <label for="email">E-mail</label>
                <input type="email" id="email" name="email" autocomplete="email" required>
                <label for="senha">Senha</label>
                <input type="password" id="senha" name="senha" autocomplete="new-password" required minlength="4">
                <label for="tipo">Perfil</label>
                <select id="tipo" name="tipo">
                    <option value="GERADOR">Gerador: tenho painel solar e quero vender</option>
                    <option value="CONSUMIDOR">Consumidor: quero comprar energia</option>
                    <option value="AMBOS">Ambos: gero, vendo e também compro</option>
                </select>
                <div class="campos-linha">
                    <div>
                        <label for="cidade">Cidade</label>
                        <input type="text" id="cidade" name="cidade" required>
                    </div>
                    <div>
                        <label for="uf">UF</label>
                        <input type="text" id="uf" name="uf" maxlength="2" required>
                    </div>
                </div>
                <button type="submit">Criar conta</button>
            </form>
            <p class="troca">Já tem conta? <a href="/login">Entrar</a></p>
            """.formatted(Layout.alerta("erro", erro));
        Http.enviarHtml(ex, 200, Layout.paginaAcesso("Criar conta", conteudo));
    }

    private void processarCadastro(HttpExchange ex) throws IOException {
        Map<String, String> form = Http.parseFormBody(ex);
        try {
            Usuario u = autenticacaoService.cadastrar(
                    form.get("nome"), form.get("email"), form.get("senha"),
                    form.get("tipo"), form.get("cidade"), form.get("uf"));
            String sessionId = SessaoManager.criarSessao(u.getId());
            Http.setCookieSessao(ex, sessionId);
            Http.redirecionar(ex, "/unidades");
        } catch (RuntimeException e) {
            exibirCadastro(ex, "Não foi possível cadastrar: e-mail já em uso.");
        }
    }
}
