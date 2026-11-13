package com.energiasolar.dao;

import com.energiasolar.model.OfertaVenda;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class OfertaVendaDAO extends AbstractDAO<OfertaVenda> {

    private static final String SQL_BASE =
            "SELECT ov.*, u.nome AS nome_vendedor, cd.nome AS nome_centro " +
            "FROM oferta_venda ov " +
            "JOIN usuario u ON u.id = ov.vendedor_id " +
            "JOIN centro_distribuicao cd ON cd.id = ov.centro_distribuicao_id ";

    @Override
    protected String nomeTabela() {
        return "oferta_venda";
    }

    @Override
    protected OfertaVenda mapear(ResultSet rs) throws SQLException {
        OfertaVenda o = new OfertaVenda();
        o.setId(rs.getInt("id"));
        o.setCreditoId(rs.getInt("credito_id"));
        o.setVendedorId(rs.getInt("vendedor_id"));
        o.setCentroDistribuicaoId(rs.getInt("centro_distribuicao_id"));
        o.setQuantidadeKwh(rs.getDouble("quantidade_kwh"));
        o.setQuantidadeDisponivelKwh(rs.getDouble("quantidade_disponivel_kwh"));
        o.setPrecoKwh(rs.getDouble("preco_kwh"));
        o.setStatus(rs.getString("status"));
        o.setDataCriacao(rs.getString("data_criacao"));
        o.setNomeVendedor(rs.getString("nome_vendedor"));
        o.setNomeCentro(rs.getString("nome_centro"));
        return o;
    }

    @Override
    public List<OfertaVenda> listarTodos() {
        return consultar(SQL_BASE + "ORDER BY ov.data_criacao DESC");
    }

    @Override
    public Optional<OfertaVenda> buscarPorId(int id) {
        return consultarUm(SQL_BASE + "WHERE ov.id = ?", id);
    }

    public OfertaVenda inserir(OfertaVenda o) {
        String sql = "INSERT INTO oferta_venda " +
                     "(credito_id, vendedor_id, centro_distribuicao_id, quantidade_kwh, " +
                     "quantidade_disponivel_kwh, preco_kwh, status) VALUES (?, ?, ?, ?, ?, ?, 'ABERTA')";
        int id = inserirRetornandoId(sql, "Erro ao criar oferta de venda",
                o.getCreditoId(), o.getVendedorId(), o.getCentroDistribuicaoId(),
                o.getQuantidadeKwh(), o.getQuantidadeDisponivelKwh(), o.getPrecoKwh());
        o.setId(id);
        return o;
    }

    public List<OfertaVenda> listarAbertasPorCentro(int centroId, int usuarioIdExcluir) {
        return consultar(SQL_BASE +
                "WHERE ov.centro_distribuicao_id = ? AND ov.status = 'ABERTA' AND ov.vendedor_id != ? " +
                "ORDER BY ov.preco_kwh ASC", centroId, usuarioIdExcluir);
    }

    public List<OfertaVenda> listarAbertasPorCentro(int centroId) {
        return consultar(SQL_BASE +
                "WHERE ov.centro_distribuicao_id = ? AND ov.status = 'ABERTA' ORDER BY ov.preco_kwh ASC", centroId);
    }

    public List<OfertaVenda> listarPorVendedor(int vendedorId) {
        return consultar(SQL_BASE + "WHERE ov.vendedor_id = ? ORDER BY ov.data_criacao DESC", vendedorId);
    }

    public void atualizarSaldo(int ofertaId, double novoSaldo, String novoStatus) {
        executarAtualizacao(
                "UPDATE oferta_venda SET quantidade_disponivel_kwh = ?, status = ? WHERE id = ?",
                "Erro ao atualizar saldo da oferta", novoSaldo, novoStatus, ofertaId);
    }

    /** Situação das ofertas lastreadas nos créditos de uma unidade consumidora. */
    public record ResumoPorUnidade(int total, int abertas, int comVendas) {
        public boolean possuiOfertas() {
            return total > 0;
        }
    }

    private static final String DA_UNIDADE =
            "FROM oferta_venda ov JOIN credito_energia c ON c.id = ov.credito_id WHERE c.unidade_id = ? ";

    public ResumoPorUnidade resumoPorUnidade(int unidadeId) {
        int total = contar("SELECT COUNT(*) " + DA_UNIDADE, unidadeId);
        int abertas = contar("SELECT COUNT(*) " + DA_UNIDADE + "AND ov.status = 'ABERTA'", unidadeId);
        int comVendas = contar("SELECT COUNT(*) " + DA_UNIDADE +
                "AND EXISTS (SELECT 1 FROM transacao t WHERE t.oferta_id = ov.id)", unidadeId);
        return new ResumoPorUnidade(total, abertas, comVendas);
    }

    public void atualizarPreco(int ofertaId, double novoPreco) {
        executarAtualizacao("UPDATE oferta_venda SET preco_kwh = ? WHERE id = ?",
                "Erro ao atualizar o preço da oferta", novoPreco, ofertaId);
    }
}
