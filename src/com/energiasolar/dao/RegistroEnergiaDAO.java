package com.energiasolar.dao;

import com.energiasolar.model.RegistroEnergia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class RegistroEnergiaDAO extends AbstractDAO<RegistroEnergia> {

    @Override
    protected String nomeTabela() {
        return "registro_energia";
    }

    @Override
    protected RegistroEnergia mapear(ResultSet rs) throws SQLException {
        RegistroEnergia r = new RegistroEnergia();
        r.setId(rs.getInt("id"));
        r.setUnidadeId(rs.getInt("unidade_id"));
        r.setMesReferencia(rs.getString("mes_referencia"));
        r.setEnergiaGeradaKwh(rs.getDouble("energia_gerada_kwh"));
        r.setEnergiaConsumidaKwh(rs.getDouble("energia_consumida_kwh"));
        r.setExcedenteKwh(rs.getDouble("excedente_kwh"));
        return r;
    }

    public RegistroEnergia inserir(RegistroEnergia r) {
        String sql = "INSERT INTO registro_energia " +
                     "(unidade_id, mes_referencia, energia_gerada_kwh, energia_consumida_kwh, excedente_kwh) " +
                     "VALUES (?, ?, ?, ?, ?)";
        int id = inserirRetornandoId(sql,
                "Erro ao inserir registro de energia (mês já lançado para essa unidade?)",
                r.getUnidadeId(), r.getMesReferencia(), r.getEnergiaGeradaKwh(),
                r.getEnergiaConsumidaKwh(), r.getExcedenteKwh());
        r.setId(id);
        return r;
    }

    public List<RegistroEnergia> listarPorUnidade(int unidadeId) {
        return consultar("SELECT * FROM registro_energia WHERE unidade_id = ? ORDER BY mes_referencia DESC",
                unidadeId);
    }

    public boolean existeNoMes(int unidadeId, String mesReferencia) {
        return contar("SELECT COUNT(*) FROM registro_energia WHERE unidade_id = ? AND mes_referencia = ?",
                unidadeId, mesReferencia) > 0;
    }
}
