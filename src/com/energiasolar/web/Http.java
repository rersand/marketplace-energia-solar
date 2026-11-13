package com.energiasolar.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Http {

    public static Map<String, String> parseFormBody(HttpExchange ex) throws IOException {
        InputStream is = ex.getRequestBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        String body = buffer.toString(StandardCharsets.UTF_8);
        return parseQueryString(body);
    }

    public static Map<String, String> parseQueryString(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;
        for (String par : query.split("&")) {
            String[] kv = par.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            params.put(key, value);
        }
        return params;
    }

    public static Map<String, String> parseCookies(HttpExchange ex) {
        Map<String, String> cookies = new HashMap<>();
        java.util.List<String> headers = ex.getRequestHeaders().get("Cookie");
        if (headers == null) return cookies;
        for (String header : headers) {
            for (String parte : header.split(";")) {
                String[] kv = parte.trim().split("=", 2);
                if (kv.length == 2) cookies.put(kv[0], kv[1]);
            }
        }
        return cookies;
    }

    public static void enviarHtml(HttpExchange ex, int status, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    public static void redirecionar(HttpExchange ex, String local) throws IOException {
        ex.getResponseHeaders().set("Location", local);
        ex.sendResponseHeaders(302, -1);
        ex.close();
    }

    public static void setCookieSessao(HttpExchange ex, String sessionId) {
        ex.getResponseHeaders().add("Set-Cookie", "SESSIONID=" + sessionId + "; Path=/; HttpOnly");
    }

    public static void limparCookieSessao(HttpExchange ex) {
        ex.getResponseHeaders().add("Set-Cookie", "SESSIONID=; Path=/; Max-Age=0; HttpOnly");
    }

    /** Neutraliza caracteres especiais de HTML em textos informados pelo usuário. */
    public static String escapar(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    /** Lê o valor inteiro de um parâmetro da URL, ou -1 se ausente ou inválido. */
    public static int parametroInteiro(HttpExchange ex, String nome) {
        String valor = parseQueryString(ex.getRequestURI().getRawQuery()).get(nome);
        try {
            return valor == null ? -1 : Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Lê o texto de um parâmetro da URL, ou null se ausente. */
    public static String parametro(HttpExchange ex, String nome) {
        return parseQueryString(ex.getRequestURI().getRawQuery()).get(nome);
    }
}
