package com.energiasolar.web;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessaoManager {

    private static final Map<String, Integer> sessoes = new ConcurrentHashMap<>();

    public static String criarSessao(int usuarioId) {
        String id = UUID.randomUUID().toString();
        sessoes.put(id, usuarioId);
        return id;
    }

    public static Integer usuarioDaSessao(String sessionId) {
        if (sessionId == null) return null;
        return sessoes.get(sessionId);
    }

    public static void encerrarSessao(String sessionId) {
        if (sessionId != null) sessoes.remove(sessionId);
    }
}
