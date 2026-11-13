package com.energiasolar.web;

import com.energiasolar.model.Usuario;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Monta o HTML comum a todas as páginas: barra superior, menu lateral (gaveta), submenu das
 * áreas, conteúdo e rodapé.
 * Também reúne os formatadores de exibição (datas, meses e selos de perfil) usados pelas telas.
 */
public class Layout {

    private static final String SOL = """
            <svg width="26" height="26" viewBox="0 0 26 26" aria-hidden="true">
                <circle cx="13" cy="13" r="5.5" fill="#ffc233"/>
                <g stroke="#ffc233" stroke-width="2" stroke-linecap="round">
                    <path d="M13 1.5v3M13 21.5v3M1.5 13h3M21.5 13h3M4.9 4.9l2.1 2.1M19 19l2.1 2.1M4.9 21.1L7 19M19 7l2.1-2.1"/>
                </g>
            </svg>""";

    private static final String SETA = """
            <svg class="seta" width="14" height="14" viewBox="0 0 14 14" aria-hidden="true"><path d="M5 3l4 4-4 4" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>""";

    private record Item(String rota, String rotulo) { }

    /**
     * Página interna. A cor de destaque, os itens do menu lateral e o submenu de cada área
     * dependem do perfil do usuário: a classe CSS vem do tipo (GERADOR, CONSUMIDOR ou AMBOS)
     * e os itens de compra e venda aparecem conforme {@link Usuario#podeComprar()} e
     * {@link Usuario#podeOfertar()}.
     *
     * @param rotaAtiva rota da página, usada para destacar o item do menu e montar o submenu da área
     */
    public static String pagina(String titulo, Usuario usuario, String rotaAtiva, String conteudo) {
        String classe = "perfil-" + usuario.getTipo().toLowerCase(Locale.ROOT);
        String perfil = Http.escapar(usuario.rotuloPerfil().toLowerCase(new Locale("pt", "BR")));
        return documento(titulo, classe, """
                <header class="barra">
                    <div class="barra-interna">
                        <button type="button" id="abrir-menu" class="hamburguer" aria-controls="gaveta" aria-expanded="false">
                            <svg width="22" height="22" viewBox="0 0 22 22" aria-hidden="true"><path d="M3 6h16M3 11h16M3 16h16" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
                            <span>Menu</span>
                        </button>
                        <a class="marca" href="/marketplace">%s Marketplace de Energia Solar</a>
                        <span class="area">Área do %s</span>
                        <div class="conta"><strong>%s</strong><a href="/logout">Sair</a></div>
                    </div>
                </header>
                %s
                <main class="conteudo">
                    %s
                </main>
                <footer class="rodape">
                    <p>Projeto acadêmico ODS 9: marketplace de créditos de energia solar excedente (Lei 14.300/2022).</p>
                </footer>
                <script>
                    (function () {
                        var corpo = document.body, botao = document.getElementById('abrir-menu'), gaveta = document.getElementById('gaveta');
                        function alternar(aberta) {
                            corpo.classList.toggle('gaveta-aberta', aberta);
                            botao.setAttribute('aria-expanded', aberta ? 'true' : 'false');
                            if (aberta) gaveta.querySelector('.fechar').focus(); else botao.focus();
                        }
                        botao.addEventListener('click', function () { alternar(true); });
                        document.querySelectorAll('[data-fechar-menu]').forEach(function (e) {
                            e.addEventListener('click', function () { alternar(false); });
                        });
                        document.addEventListener('keydown', function (e) {
                            if (e.key === 'Escape' && corpo.classList.contains('gaveta-aberta')) alternar(false);
                        });
                    })();
                </script>
                """.formatted(SOL, perfil, Http.escapar(usuario.getNome()), gaveta(usuario, rotaAtiva, perfil),
                    emArea(usuario, rotaAtiva, conteudo)));
    }

    private static List<Item> itensVendas() {
        return List.of(new Item("/vendas", "Resumo"), new Item("/vendas/ofertas", "Minhas ofertas"),
                new Item("/vendas/estoque", "Estoque de créditos"));
    }

    private static List<Item> itensEnergia(Usuario usuario) {
        return List.of(new Item("/unidades", "Unidades consumidoras"),
                new Item("/energia", usuario.podeOfertar() ? "Registrar energia" : "Registrar consumo"));
    }

    private static boolean contem(List<Item> itens, String rota) {
        for (Item i : itens) if (i.rota().equals(rota)) return true;
        return false;
    }

    private static String gaveta(Usuario usuario, String rotaAtiva, String perfil) {
        StringBuilder html = new StringBuilder();
        html.append(link("/marketplace", "Início", rotaAtiva));
        if (usuario.podeComprar()) html.append(link("/compras", "Minhas compras", rotaAtiva));
        if (usuario.podeOfertar()) html.append(grupo("Vendas", itensVendas(), rotaAtiva));
        html.append(grupo("Minha energia", itensEnergia(usuario), rotaAtiva));
        html.append(link("/relatorios", "Relatórios", rotaAtiva));

        String inicial = usuario.getNome() == null || usuario.getNome().isBlank() ? "?"
                : usuario.getNome().trim().substring(0, 1).toUpperCase(new Locale("pt", "BR"));
        return """
            <div class="gaveta-fundo" data-fechar-menu></div>
            <nav id="gaveta" class="gaveta" aria-label="Menu principal">
                <div class="gaveta-topo">
                    <span class="avatar" aria-hidden="true">%s</span>
                    <div><strong>%s</strong><span>Área do %s</span></div>
                    <button type="button" class="fechar" data-fechar-menu aria-label="Fechar menu">&times;</button>
                </div>
                <div class="gaveta-itens">%s</div>
                <a class="gaveta-sair" href="/logout">Sair</a>
            </nav>
            """.formatted(Http.escapar(inicial), Http.escapar(usuario.getNome()), perfil, html);
    }

    private static String link(String rota, String rotulo, String rotaAtiva) {
        boolean ativo = rota.equals(rotaAtiva);
        return "<a class=\"gaveta-item%s\" href=\"%s\"%s>%s</a>".formatted(ativo ? " ativo" : "", rota,
                ativo ? " aria-current=\"page\"" : "", rotulo);
    }

    private static String grupo(String titulo, List<Item> itens, String rotaAtiva) {
        StringBuilder sub = new StringBuilder();
        for (Item i : itens) sub.append(link(i.rota(), i.rotulo(), rotaAtiva));
        return """
            <details class="gaveta-grupo"%s>
                <summary>%s%s</summary>
                <div class="gaveta-sub">%s</div>
            </details>
            """.formatted(contem(itens, rotaAtiva) ? " open" : "", titulo, SETA, sub);
    }

    /** Páginas de Vendas e de Minha energia ganham o submenu lateral da área e a trilha de navegação. */
    private static String emArea(Usuario usuario, String rotaAtiva, String conteudo) {
        String nomeArea;
        List<Item> itens;
        if (contem(itensVendas(), rotaAtiva)) {
            nomeArea = "Vendas";
            itens = itensVendas();
        } else if (contem(itensEnergia(usuario), rotaAtiva)) {
            nomeArea = "Minha energia";
            itens = itensEnergia(usuario);
        } else {
            return conteudo;
        }

        StringBuilder sub = new StringBuilder();
        String atual = "";
        for (Item i : itens) {
            boolean ativo = i.rota().equals(rotaAtiva);
            if (ativo) atual = i.rotulo();
            sub.append("<a href=\"%s\"%s>%s</a>".formatted(i.rota(), ativo ? " class=\"ativo\" aria-current=\"page\"" : "", i.rotulo()));
        }
        String atalho = nomeArea.equals("Vendas") ? "<a class=\"botao subnav-acao\" href=\"/vendas/ofertas#nova-oferta\">Anunciar crédito</a>" : "";
        return """
            <div class="area-layout">
                <aside class="subnav" aria-label="%s">
                    <p class="subnav-titulo">%s</p>
                    <nav>%s</nav>
                    %s
                </aside>
                <div class="area-conteudo">
                    <p class="trilha"><span>%s</span>%s<span>%s</span></p>
                    %s
                </div>
            </div>
            """.formatted(nomeArea, nomeArea, sub, atalho, nomeArea, SETA, atual, conteudo);
    }

    /** Página de login e cadastro: apresentação do marketplace à esquerda e formulário à direita. */
    public static String paginaAcesso(String titulo, String formulario) {
        return documento(titulo, """
                <div class="acesso">
                    <section class="acesso-apresentacao">
                        <div>
                            <span class="marca">%s Marketplace de Energia Solar</span>
                        </div>
                        <div>
                            <h1>Venda o excedente do seu telhado para quem está perto de você.</h1>
                            <p class="lead">A energia que sobra no mês vira crédito na distribuidora. Aqui você oferta esse crédito
                            para consumidores do mesmo centro de distribuição, pelo preço que definir.</p>
                        </div>
                        <ol class="como-funciona">
                            <li><span class="etapa">1</span><div><b>Cadastre a unidade consumidora</b>O número da UC e o centro de distribuição que a atende.</div></li>
                            <li><span class="etapa">2</span><div><b>Lance geração e consumo do mês</b>O que sobrar vira crédito válido por 60 meses.</div></li>
                            <li><span class="etapa">3</span><div><b>Publique ou compre ofertas</b>As negociações acontecem dentro da mesma região.</div></li>
                        </ol>
                        <p class="rodape-acesso">Projeto acadêmico ODS 9: Indústria, Inovação e Infraestrutura.</p>
                    </section>
                    <section class="acesso-formulario">
                        <div class="caixa-acesso">%s</div>
                    </section>
                </div>
                """.formatted(SOL, formulario));
    }

    private static String documento(String titulo, String corpo) {
        return documento(titulo, "", corpo);
    }

    private static String documento(String titulo, String classeCorpo, String corpo) {
        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s | Marketplace de Energia Solar</title>
                <link rel="stylesheet" href="/style.css">
            </head>
            <body%s>
            %s
            </body>
            </html>
            """.formatted(Http.escapar(titulo), classeCorpo.isEmpty() ? "" : " class=\"" + classeCorpo + "\"", corpo);
    }

    public static String alerta(String tipo, String mensagem) {
        if (mensagem == null || mensagem.isEmpty()) return "";
        return "<div class=\"alerta alerta-" + tipo + "\">" + Http.escapar(mensagem) + "</div>";
    }

    public static String selo(String tipo, String rotulo) {
        String classe = switch (tipo == null ? "" : tipo) {
            case "GERADOR" -> "selo selo-gerador";
            case "AMBOS" -> "selo selo-ambos";
            default -> "selo";
        };
        return "<span class=\"" + classe + "\">" + Http.escapar(rotulo) + "</span>";
    }

    public static String vazio(String titulo, String texto, String link, String rotuloLink) {
        String botao = link == null ? "" : "<a class=\"botao\" href=\"" + link + "\">" + rotuloLink + "</a>";
        return "<div class=\"vazio\"><strong>" + titulo + "</strong>" + texto + botao + "</div>";
    }

    /** Converte "2031-11-11" (ou "2026-11-11 10:30:00") para "11/11/2031" (ou "11/11/2026 10:30"). */
    public static String data(String iso) {
        if (iso == null || iso.length() < 10) return Http.escapar(iso);
        try {
            String dia = LocalDate.parse(iso.substring(0, 10)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            return iso.length() >= 16 ? dia + " " + iso.substring(11, 16) : dia;
        } catch (Exception e) {
            return Http.escapar(iso);
        }
    }

    /** Converte "2026-11" para "nov/2026". */
    public static String mes(String anoMes) {
        try {
            return YearMonth.parse(anoMes).format(DateTimeFormatter.ofPattern("MMM/yyyy", new Locale("pt", "BR")))
                    .replace(".", "");
        } catch (Exception e) {
            return Http.escapar(anoMes);
        }
    }
}
