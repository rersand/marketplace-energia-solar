package com.energiasolar.web;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServidorWeb {

    public static void iniciar(int porta, String pastaWebapp) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(porta), 0);

        servidor.createContext("/login", new LoginHandler());
        servidor.createContext("/cadastro", new LoginHandler());
        servidor.createContext("/logout", new LoginHandler());
        servidor.createContext("/unidades", new UnidadeHandler());
        servidor.createContext("/energia", new EnergiaHandler());
        servidor.createContext("/marketplace", new MarketplaceHandler());
        servidor.createContext("/vendas", new VendasHandler());
        servidor.createContext("/compras", new ComprasHandler());
        servidor.createContext("/relatorios", new RelatorioHandler());

        servidor.createContext("/style.css", exchange -> {
            Path arquivo = Path.of(pastaWebapp, "style.css");
            byte[] bytes = Files.readAllBytes(arquivo);
            exchange.getResponseHeaders().set("Content-Type", "text/css; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        });

        servidor.createContext("/", exchange -> Http.redirecionar(exchange, "/marketplace"));

        servidor.setExecutor(null);
        servidor.start();

        System.out.println("[SERVIDOR] Rodando em http://localhost:" + porta);
    }
}
