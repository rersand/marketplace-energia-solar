package com.energiasolar.service;

import com.energiasolar.dao.UsuarioDAO;
import com.energiasolar.model.Usuario;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * Regras de autenticação e cadastro de participantes.
 *
 * As senhas são armazenadas como hash SHA-256 combinado a um salt aleatório por usuário,
 * nunca em texto puro. Em produção recomenda-se um algoritmo específico para senhas
 * (BCrypt, Argon2 ou PBKDF2), propositalmente lentos para dificultar força bruta.
 */
public class AutenticacaoService {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public Usuario cadastrar(String nome, String email, String senha, String tipo, String cidade, String uf) {
        String salt = gerarSalt();
        String hash = gerarHash(senha, salt);

        Usuario u = Usuario.novoPerfil(tipo);
        u.setNome(nome);
        u.setEmail(email);
        u.setSenhaHash(hash);
        u.setSenhaSalt(salt);
        u.setCidade(cidade);
        u.setUf(uf);
        return usuarioDAO.inserir(u);
    }

    public Optional<Usuario> autenticar(String email, String senha) {
        Optional<Usuario> usuarioOpt = usuarioDAO.buscarPorEmail(email);
        if (usuarioOpt.isEmpty()) return Optional.empty();

        Usuario u = usuarioOpt.get();
        String hashCalculado = gerarHash(senha, u.getSenhaSalt());
        if (hashCalculado.equals(u.getSenhaHash())) {
            return Optional.of(u);
        }
        return Optional.empty();
    }

    private String gerarSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String gerarHash(String senha, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Base64.getDecoder().decode(salt));
            byte[] hash = digest.digest(senha.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Erro ao gerar hash de senha", e);
        }
    }
}
