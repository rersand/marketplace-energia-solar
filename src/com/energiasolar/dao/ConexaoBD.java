package com.energiasolar.dao;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

/**
 * Fábrica de conexões com o banco de dados.
 *
 * Utiliza SQLite, banco embarcado que não exige instalação nem configuração de servidor,
 * garantindo que o sistema execute de forma idêntica em qualquer ambiente.
 *
 * Para migrar a outro SGBD basta alterar URL_BANCO e adicionar o driver correspondente ao
 * classpath: o restante do código usa apenas JDBC padrão e não precisa ser modificado.
 */
public class ConexaoBD {

    private static final String URL_BANCO = "jdbc:sqlite:energiasolar.db";

    private static final ThreadLocal<Connection> TRANSACAO_ATUAL = new ThreadLocal<>();

    /**
     * Devolve uma conexão com o banco.
     * Dentro de {@link #emTransacao(Supplier)} devolve sempre a conexão da transação em
     * curso, protegida contra fechamento, para que todas as DAOs participem dela.
     */
    public static Connection obterConexao() throws SQLException {
        Connection transacao = TRANSACAO_ATUAL.get();
        if (transacao != null) {
            return protegidaContraFechamento(transacao);
        }
        return abrirConexao();
    }

    /**
     * Executa a operação como uma unidade atômica: ou todas as gravações são confirmadas,
     * ou nenhuma é. Qualquer exceção lançada pela operação desfaz o que já foi gravado.
     */
    public static <T> T emTransacao(Supplier<T> operacao) {
        if (TRANSACAO_ATUAL.get() != null) {
            return operacao.get();
        }
        try (Connection conexao = abrirConexao()) {
            conexao.setAutoCommit(false);
            TRANSACAO_ATUAL.set(conexao);
            try {
                T resultado = operacao.get();
                conexao.commit();
                return resultado;
            } catch (RuntimeException e) {
                conexao.rollback();
                throw e;
            } finally {
                TRANSACAO_ATUAL.remove();
                conexao.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao controlar a transação no banco de dados", e);
        }
    }

    private static Connection abrirConexao() throws SQLException {
        Connection conexao = DriverManager.getConnection(URL_BANCO);
        try (Statement stmt = conexao.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conexao;
    }

    private static Connection protegidaContraFechamento(Connection real) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, metodo, argumentos) -> {
                    if ("close".equals(metodo.getName())) {
                        return null;
                    }
                    try {
                        return metodo.invoke(real, argumentos);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    public static int ultimoIdInserido(Connection conexao) throws SQLException {
        try (Statement stmt = conexao.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public static void inicializarBanco(String caminhoSchema) {
        try (Connection conexao = obterConexao();
             Statement stmt = conexao.createStatement()) {

            String sql = Files.readString(Paths.get(caminhoSchema));
            for (String comando : sql.split(";")) {
                String cmd = comando.trim();
                if (!cmd.isEmpty()) {
                    stmt.execute(cmd);
                }
            }
            System.out.println("[BD] Schema inicializado com sucesso.");
        } catch (SQLException | IOException e) {
            throw new RuntimeException("Falha ao inicializar o banco de dados", e);
        }
    }
}
