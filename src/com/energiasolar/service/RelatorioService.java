package com.energiasolar.service;

import com.energiasolar.dao.TransacaoDAO;
import com.energiasolar.model.Transacao;

import java.util.List;

public class RelatorioService {

    private final TransacaoDAO transacaoDAO = new TransacaoDAO();

    public List<Object[]> relatorioPorCentroDistribuicao() {
        return transacaoDAO.relatorioPorCentro();
    }

    public List<Transacao> extratoComprasDoUsuario(int usuarioId) {
        return transacaoDAO.listarPorComprador(usuarioId);
    }

    public List<Transacao> extratoVendasDoUsuario(int usuarioId) {
        return transacaoDAO.listarPorVendedor(usuarioId);
    }
}
