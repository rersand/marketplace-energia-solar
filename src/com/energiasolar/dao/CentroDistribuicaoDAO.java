package com.energiasolar.dao;

import com.energiasolar.model.CentroDistribuicao;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CentroDistribuicaoDAO extends AbstractDAO<CentroDistribuicao> {

    @Override
    protected String nomeTabela() {
        return "centro_distribuicao";
    }

    @Override
    protected String ordenacaoPadrao() {
        return "nome";
    }

    @Override
    protected CentroDistribuicao mapear(ResultSet rs) throws SQLException {
        return new CentroDistribuicao(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("cidade"),
                rs.getString("uf"),
                rs.getDouble("capacidade_mwh")
        );
    }
}
