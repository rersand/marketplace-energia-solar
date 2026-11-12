package com.energiasolar;

import java.lang.reflect.Method;

/**
 * Ponto de entrada do jar executável.
 *
 * É compilado para Java 8, enquanto o restante do sistema exige Java 17. Assim, quando o
 * sistema é aberto com uma versão antiga do Java, esta classe ainda consegue executar e
 * exibe uma mensagem clara de como resolver, em vez do erro técnico
 * UnsupportedClassVersionError que a JVM produziria ao carregar as demais classes.
 */
public class Inicializador {

    private static final int VERSAO_MINIMA = 17;

    public static void main(String[] args) throws Exception {
        int versao = versaoDoJava();
        if (versao < VERSAO_MINIMA) {
            System.err.println("ERRO: este sistema exige Java " + VERSAO_MINIMA + " ou superior.");
            System.err.println("Versao encontrada: Java " + versao + " (" + System.getProperty("java.home") + ")");
            System.err.println("Instale o JDK 21 LTS em https://adoptium.net e execute novamente.");
            System.exit(1);
        }
        Method main = Class.forName("com.energiasolar.Main").getMethod("main", String[].class);
        try {
            main.invoke(null, (Object) args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (e.getCause() instanceof Exception) ? (Exception) e.getCause() : e;
        }
    }

    private static int versaoDoJava() {
        String especificacao = System.getProperty("java.specification.version");
        if (especificacao.startsWith("1.")) {
            especificacao = especificacao.substring(2);
        }
        int ponto = especificacao.indexOf('.');
        if (ponto > 0) {
            especificacao = especificacao.substring(0, ponto);
        }
        return Integer.parseInt(especificacao);
    }
}
