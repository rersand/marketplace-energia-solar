package com.energiasolar.web;

import com.energiasolar.dao.UsuarioDAO;
import com.energiasolar.model.Usuario;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;
import java.util.Optional;

public class Autenticacao {

    private static final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public static Usuario usuarioLogado(HttpExchange ex) {
        Map<String, String> cookies = Http.parseCookies(ex);
        Integer usuarioId = SessaoManager.usuarioDaSessao(cookies.get("SESSIONID"));
        if (usuarioId == null) return null;
        Optional<Usuario> u = usuarioDAO.buscarPorId(usuarioId);
        return u.orElse(null);
    }
}
