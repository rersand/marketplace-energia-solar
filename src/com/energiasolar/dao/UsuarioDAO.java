package com.energiasolar.dao;

import com.energiasolar.model.Usuario;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UsuarioDAO extends AbstractDAO<Usuario> {

    @Override
    protected String nomeTabela() {
        return "usuario";
    }

    @Override
    protected String ordenacaoPadrao() {
        return "nome";
    }

    @Override
    /**
     * Converte a linha do banco na subclasse correta de Usuario.
     * A tabela é única (Single Table Inheritance) e a coluna 'tipo' atua como
     * discriminador: o objeto devolvido é um Gerador, um Consumidor ou um Prosumidor.
     */
    protected Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = Usuario.novoPerfil(rs.getString("tipo"));
        u.setId(rs.getInt("id"));
        u.setNome(rs.getString("nome"));
        u.setEmail(rs.getString("email"));
        u.setSenhaHash(rs.getString("senha_hash"));
        u.setSenhaSalt(rs.getString("senha_salt"));
        u.setCidade(rs.getString("cidade"));
        u.setUf(rs.getString("uf"));
        return u;
    }

    public Usuario inserir(Usuario u) {
        String sql = "INSERT INTO usuario (nome, email, senha_hash, senha_salt, tipo, cidade, uf) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        int id = inserirRetornandoId(sql, "Erro ao inserir usuário (e-mail já cadastrado?)",
                u.getNome(), u.getEmail(), u.getSenhaHash(), u.getSenhaSalt(),
                u.getTipo(), u.getCidade(), u.getUf());
        u.setId(id);
        return u;
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return consultarUm("SELECT * FROM usuario WHERE email = ?", email);
    }
}
