package com.energiasolar.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Superclasse abstrata de todos os DAOs do sistema.
 *
 * Aplica o padrão Template Method: implementa o esqueleto invariante das operações de
 * acesso a dados (abrir conexão, preparar comando, percorrer o ResultSet, tratar erro e
 * liberar recursos) e delega às subclasses as duas decisões que variam, declaradas como
 * métodos abstratos: {@link #nomeTabela()} e {@link #mapear(java.sql.ResultSet)}.
 *
 * Quando {@link #listarTodos()} invoca mapear(), o objeto efetivamente produzido é
 * determinado em tempo de execução pela subclasse concreta — o mesmo código devolve um
 * Usuario, um CreditoEnergia ou uma Transacao conforme o contexto.
 *
 * @param <T> a entidade de domínio que a DAO concreta manipula.
 */
public abstract class AbstractDAO<T> {

    /** Nome da tabela que esta DAO representa. */
    protected abstract String nomeTabela();

    /** Converte a linha atual do ResultSet na entidade correspondente. */
    protected abstract T mapear(ResultSet rs) throws SQLException;

    /** Coluna de ordenação padrão de listarTodos(); pode ser sobrescrita. */
    protected String ordenacaoPadrao() {
        return "id";
    }

    /** Lista todos os registros da tabela, já convertidos em entidades. */
    public List<T> listarTodos() {
        return consultar("SELECT * FROM " + nomeTabela() + " ORDER BY " + ordenacaoPadrao());
    }

    /** Busca um registro pela chave primária. */
    public Optional<T> buscarPorId(int id) {
        return consultarUm("SELECT * FROM " + nomeTabela() + " WHERE id = ?", id);
    }

    /** Remove um registro pela chave primária. */
    public void excluirPorId(int id) {
        executarAtualizacao("DELETE FROM " + nomeTabela() + " WHERE id = ?",
                "Erro ao excluir registro de " + nomeTabela(), id);
    }

    /** Executa uma consulta de contagem e devolve o valor da primeira coluna. */
    protected int contar(String sql, Object... parametros) {
        try (Connection con = ConexaoBD.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            aplicarParametros(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar registros de " + nomeTabela(), e);
        }
    }

    protected List<T> consultar(String sql, Object... parametros) {
        List<T> lista = new ArrayList<>();
        try (Connection con = ConexaoBD.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            aplicarParametros(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar " + nomeTabela(), e);
        }
        return lista;
    }

    protected Optional<T> consultarUm(String sql, Object... parametros) {
        try (Connection con = ConexaoBD.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            aplicarParametros(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar " + nomeTabela(), e);
        }
        return Optional.empty();
    }

    protected int inserirRetornandoId(String sql, String mensagemErro, Object... parametros) {
        try (Connection con = ConexaoBD.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            aplicarParametros(ps, parametros);
            ps.executeUpdate();
            return ConexaoBD.ultimoIdInserido(con);
        } catch (SQLException e) {
            throw new RuntimeException(mensagemErro, e);
        }
    }

    protected void executarAtualizacao(String sql, String mensagemErro, Object... parametros) {
        try (Connection con = ConexaoBD.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            aplicarParametros(ps, parametros);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(mensagemErro, e);
        }
    }

    private void aplicarParametros(PreparedStatement ps, Object... parametros) throws SQLException {
        for (int i = 0; i < parametros.length; i++) {
            ps.setObject(i + 1, parametros[i]);
        }
    }
}
