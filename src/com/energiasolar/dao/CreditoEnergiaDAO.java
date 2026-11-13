package com.energiasolar.dao;

import com.energiasolar.model.CreditoEnergia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class CreditoEnergiaDAO extends AbstractDAO<CreditoEnergia> {

    @Override
    protected String nomeTabela() {
        return "credito_energia";
    }

    @Override
    protected CreditoEnergia mapear(ResultSet rs) throws SQLException {
        CreditoEnergia c = new CreditoEnergia();
        c.setId(rs.getInt("id"));
        c.setUnidadeId(rs.getInt("unidade_id"));
        c.setRegistroId(rs.getInt("registro_id"));
        c.setQuantidadeKwh(rs.getDouble("quantidade_kwh"));
        c.setQuantidadeDisponivelKwh(rs.getDouble("quantidade_disponivel_kwh"));
        c.setDataValidade(rs.getString("data_validade"));
        c.setStatus(rs.getString("status"));
        return c;
    }

    public CreditoEnergia inserir(CreditoEnergia c) {
        String sql = "INSERT INTO credito_energia " +
                     "(unidade_id, registro_id, quantidade_kwh, quantidade_disponivel_kwh, data_validade, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        int id = inserirRetornandoId(sql, "Erro ao inserir crédito de energia",
                c.getUnidadeId(), c.getRegistroId(), c.getQuantidadeKwh(),
                c.getQuantidadeDisponivelKwh(), c.getDataValidade(), c.getStatus());
        c.setId(id);
        return c;
    }

    public List<CreditoEnergia> listarDisponiveisPorUnidade(int unidadeId) {
        return consultar("SELECT * FROM credito_energia WHERE unidade_id = ? " +
                "AND status IN ('DISPONIVEL','PARCIAL') AND quantidade_disponivel_kwh > 0 " +
                "ORDER BY data_validade", unidadeId);
    }

    public List<CreditoEnergia> listarPorUnidade(int unidadeId) {
        return consultar("SELECT * FROM credito_energia WHERE unidade_id = ? ORDER BY data_geracao DESC",
                unidadeId);
    }

    public void atualizarSaldo(int creditoId, double novoSaldo, String novoStatus) {
        executarAtualizacao(
                "UPDATE credito_energia SET quantidade_disponivel_kwh = ?, status = ? WHERE id = ?",
                "Erro ao atualizar saldo do crédito", novoSaldo, novoStatus, creditoId);
    }
}
