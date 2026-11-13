package com.energiasolar.dao;

import com.energiasolar.model.UnidadeConsumidora;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class UnidadeConsumidoraDAO extends AbstractDAO<UnidadeConsumidora> {

    private static final String SQL_BASE =
            "SELECT uc.*, cd.nome AS nome_centro FROM unidade_consumidora uc " +
            "JOIN centro_distribuicao cd ON cd.id = uc.centro_distribuicao_id ";

    @Override
    protected String nomeTabela() {
        return "unidade_consumidora";
    }

    @Override
    protected UnidadeConsumidora mapear(ResultSet rs) throws SQLException {
        UnidadeConsumidora u = new UnidadeConsumidora();
        u.setId(rs.getInt("id"));
        u.setUsuarioId(rs.getInt("usuario_id"));
        u.setCentroDistribuicaoId(rs.getInt("centro_distribuicao_id"));
        u.setNumeroUc(rs.getString("numero_uc"));
        u.setEndereco(rs.getString("endereco"));
        u.setCapacidadeGeracaoKwp(rs.getDouble("capacidade_geracao_kwp"));
        u.setNomeCentro(rs.getString("nome_centro"));
        return u;
    }

    @Override
    public List<UnidadeConsumidora> listarTodos() {
        return consultar(SQL_BASE + "ORDER BY uc.id");
    }

    @Override
    public Optional<UnidadeConsumidora> buscarPorId(int id) {
        return consultarUm(SQL_BASE + "WHERE uc.id = ?", id);
    }

    public UnidadeConsumidora inserir(UnidadeConsumidora u) {
        String sql = "INSERT INTO unidade_consumidora " +
                     "(usuario_id, centro_distribuicao_id, numero_uc, endereco, capacidade_geracao_kwp) " +
                     "VALUES (?, ?, ?, ?, ?)";
        int id = inserirRetornandoId(sql, "Erro ao inserir unidade consumidora",
                u.getUsuarioId(), u.getCentroDistribuicaoId(), u.getNumeroUc(),
                u.getEndereco(), u.getCapacidadeGeracaoKwp());
        u.setId(id);
        return u;
    }

    public List<UnidadeConsumidora> listarPorUsuario(int usuarioId) {
        return consultar(SQL_BASE + "WHERE uc.usuario_id = ? ORDER BY uc.id", usuarioId);
    }

    public void atualizar(UnidadeConsumidora u) {
        executarAtualizacao(
                "UPDATE unidade_consumidora SET centro_distribuicao_id = ?, numero_uc = ?, " +
                "endereco = ?, capacidade_geracao_kwp = ? WHERE id = ?",
                "Erro ao atualizar unidade consumidora",
                u.getCentroDistribuicaoId(), u.getNumeroUc(), u.getEndereco(),
                u.getCapacidadeGeracaoKwp(), u.getId());
    }

    public Optional<UnidadeConsumidora> buscarPorNumero(String numeroUc) {
        return consultarUm(SQL_BASE + "WHERE uc.numero_uc = ?", numeroUc);
    }

    public boolean existeNoCentro(int usuarioId, int centroId) {
        return contar("SELECT COUNT(*) FROM unidade_consumidora WHERE usuario_id = ? AND centro_distribuicao_id = ?",
                usuarioId, centroId) > 0;
    }
}
