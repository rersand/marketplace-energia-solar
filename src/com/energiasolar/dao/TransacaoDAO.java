package com.energiasolar.dao;

import com.energiasolar.model.Transacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TransacaoDAO extends AbstractDAO<Transacao> {

    private static final String SQL_BASE =
            "SELECT t.*, uc.nome AS nome_comprador, uv.nome AS nome_vendedor " +
            "FROM transacao t " +
            "JOIN oferta_venda ov ON ov.id = t.oferta_id " +
            "JOIN usuario uc ON uc.id = t.comprador_id " +
            "JOIN usuario uv ON uv.id = ov.vendedor_id ";

    @Override
    protected String nomeTabela() {
        return "transacao";
    }

    @Override
    protected Transacao mapear(ResultSet rs) throws SQLException {
        Transacao t = new Transacao();
        t.setId(rs.getInt("id"));
        t.setOfertaId(rs.getInt("oferta_id"));
        t.setCompradorId(rs.getInt("comprador_id"));
        t.setQuantidadeKwh(rs.getDouble("quantidade_kwh"));
        t.setValorTotal(rs.getDouble("valor_total"));
        t.setDataTransacao(rs.getString("data_transacao"));
        t.setNomeComprador(rs.getString("nome_comprador"));
        t.setNomeVendedor(rs.getString("nome_vendedor"));
        return t;
    }

    @Override
    public List<Transacao> listarTodos() {
        return consultar(SQL_BASE + "ORDER BY t.data_transacao DESC");
    }

    @Override
    public Optional<Transacao> buscarPorId(int id) {
        return consultarUm(SQL_BASE + "WHERE t.id = ?", id);
    }

    public Transacao inserir(Transacao t) {
        String sql = "INSERT INTO transacao (oferta_id, comprador_id, quantidade_kwh, valor_total) " +
                     "VALUES (?, ?, ?, ?)";
        int id = inserirRetornandoId(sql, "Erro ao registrar transação",
                t.getOfertaId(), t.getCompradorId(), t.getQuantidadeKwh(), t.getValorTotal());
        t.setId(id);
        return t;
    }

    public List<Transacao> listarPorComprador(int compradorId) {
        return consultar(SQL_BASE + "WHERE t.comprador_id = ? ORDER BY t.data_transacao DESC", compradorId);
    }

    public List<Transacao> listarPorVendedor(int vendedorId) {
        return consultar(SQL_BASE + "WHERE ov.vendedor_id = ? ORDER BY t.data_transacao DESC", vendedorId);
    }

    public List<Object[]> relatorioPorCentro() {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT cd.nome, COALESCE(SUM(t.quantidade_kwh),0), COALESCE(SUM(t.valor_total),0), COUNT(t.id) " +
                     "FROM centro_distribuicao cd " +
                     "LEFT JOIN oferta_venda ov ON ov.centro_distribuicao_id = cd.id " +
                     "LEFT JOIN transacao t ON t.oferta_id = ov.id " +
                     "GROUP BY cd.id, cd.nome ORDER BY cd.nome";
        try (Connection con = ConexaoBD.obterConexao();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Object[]{rs.getString(1), rs.getDouble(2), rs.getDouble(3), rs.getInt(4)});
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao gerar relatório por centro", e);
        }
        return lista;
    }

    public int contarPorOferta(int ofertaId) {
        return contar("SELECT COUNT(*) FROM transacao WHERE oferta_id = ?", ofertaId);
    }

    public double totalVendidoPorOferta(int ofertaId) {
        String sql = "SELECT COALESCE(SUM(quantidade_kwh), 0) FROM transacao WHERE oferta_id = ?";
        try (Connection con = ConexaoBD.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ofertaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao somar as vendas da oferta", e);
        }
    }
}
