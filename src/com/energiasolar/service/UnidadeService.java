package com.energiasolar.service;

import com.energiasolar.dao.CentroDistribuicaoDAO;
import com.energiasolar.dao.ConexaoBD;
import com.energiasolar.dao.CreditoEnergiaDAO;
import com.energiasolar.dao.OfertaVendaDAO;
import com.energiasolar.dao.RegistroEnergiaDAO;
import com.energiasolar.dao.UnidadeConsumidoraDAO;
import com.energiasolar.model.UnidadeConsumidora;

import java.util.List;
import java.util.Optional;

/**
 * Regras de negócio do cadastro de unidades consumidoras (CRUD).
 *
 * Toda operação sobre uma unidade existente confere antes que ela pertence ao usuário
 * que a solicita. A exclusão é física, mas só é permitida quando nenhuma oferta do
 * marketplace depende dos créditos da unidade: se houver, a operação é recusada e o
 * motivo é informado. Sem dependências, a exclusão remove também, em cascata, os
 * lançamentos de energia e os créditos gerados pela unidade.
 */
public class UnidadeService {

    /** Limite da minigeração distribuída para fonte solar (3 MW), conforme a Lei 14.300/2022. */
    private static final double CAPACIDADE_MAXIMA_KWP = 3000;

    private final UnidadeConsumidoraDAO unidadeDAO = new UnidadeConsumidoraDAO();
    private final CentroDistribuicaoDAO centroDAO = new CentroDistribuicaoDAO();
    private final OfertaVendaDAO ofertaDAO = new OfertaVendaDAO();
    private final RegistroEnergiaDAO registroDAO = new RegistroEnergiaDAO();
    private final CreditoEnergiaDAO creditoDAO = new CreditoEnergiaDAO();

    /** Resultado de uma exclusão, com o que foi removido junto da unidade. */
    public record ResultadoExclusao(String numeroUc, int registrosRemovidos, int creditosRemovidos) {
    }

    public UnidadeConsumidora cadastrar(int usuarioId, int centroId, String numeroUc,
                                        String endereco, double capacidadeKwp) {
        UnidadeConsumidora u = new UnidadeConsumidora();
        u.setUsuarioId(usuarioId);
        preencherValidando(u, centroId, numeroUc, endereco, capacidadeKwp);
        return unidadeDAO.inserir(u);
    }

    public List<UnidadeConsumidora> listarDoUsuario(int usuarioId) {
        return unidadeDAO.listarPorUsuario(usuarioId);
    }

    public UnidadeConsumidora buscarDoUsuario(int unidadeId, int usuarioId) {
        Optional<UnidadeConsumidora> unidade = unidadeDAO.buscarPorId(unidadeId);
        if (unidade.isEmpty() || unidade.get().getUsuarioId() != usuarioId) {
            throw new RegraNegocioException("Unidade consumidora não encontrada.");
        }
        return unidade.get();
    }

    public UnidadeConsumidora atualizar(int unidadeId, int usuarioId, int centroId, String numeroUc,
                                        String endereco, double capacidadeKwp) {
        UnidadeConsumidora u = buscarDoUsuario(unidadeId, usuarioId);

        if (u.getCentroDistribuicaoId() != centroId) {
            int abertas = ofertaDAO.resumoPorUnidade(unidadeId).abertas();
            if (abertas > 0) {
                throw new RegraNegocioException(
                        "Não é possível transferir a unidade " + u.getNumeroUc() + " para outro centro de " +
                        "distribuição: há " + plural(abertas, "oferta aberta", "ofertas abertas") +
                        " lastreada(s) nos créditos dela, negociada(s) no centro atual. " +
                        "Cancele essas ofertas antes de transferir a unidade.");
            }
        }

        preencherValidando(u, centroId, numeroUc, endereco, capacidadeKwp);
        unidadeDAO.atualizar(u);
        return u;
    }

    public ResultadoExclusao excluir(int unidadeId, int usuarioId) {
        UnidadeConsumidora u = buscarDoUsuario(unidadeId, usuarioId);
        OfertaVendaDAO.ResumoPorUnidade ofertas = ofertaDAO.resumoPorUnidade(unidadeId);

        if (ofertas.comVendas() > 0) {
            throw new RegraNegocioException(
                    "A unidade " + u.getNumeroUc() + " não pode ser excluída: possui " +
                    plural(ofertas.comVendas(), "oferta com vendas registradas", "ofertas com vendas registradas") +
                    " no marketplace. O histórico de transações é um registro financeiro de compradores e " +
                    "vendedores e precisa ser preservado.");
        }
        if (ofertas.possuiOfertas()) {
            throw new RegraNegocioException(
                    "A unidade " + u.getNumeroUc() + " não pode ser excluída: possui " +
                    plural(ofertas.total(), "oferta publicada", "ofertas publicadas") + " no marketplace (" +
                    plural(ofertas.abertas(), "aberta", "abertas") + "), lastreada(s) nos créditos dela. " +
                    "Exclua essas ofertas em Vendas › Minhas ofertas e tente novamente.");
        }

        return ConexaoBD.emTransacao(() -> {
            int registros = registroDAO.listarPorUnidade(unidadeId).size();
            int creditos = creditoDAO.listarPorUnidade(unidadeId).size();
            unidadeDAO.excluirPorId(unidadeId);
            return new ResultadoExclusao(u.getNumeroUc(), registros, creditos);
        });
    }

    private void preencherValidando(UnidadeConsumidora u, int centroId, String numeroUc,
                                    String endereco, double capacidadeKwp) {
        String numero = numeroUc == null ? "" : numeroUc.trim();
        String end = endereco == null ? "" : endereco.trim();

        if (numero.length() < 3 || numero.length() > 30) {
            throw new RegraNegocioException("O número da UC deve ter entre 3 e 30 caracteres.");
        }
        if (end.length() < 5 || end.length() > 200) {
            throw new RegraNegocioException("O endereço deve ter entre 5 e 200 caracteres.");
        }
        if (capacidadeKwp < 0 || capacidadeKwp > CAPACIDADE_MAXIMA_KWP) {
            throw new RegraNegocioException(
                    "A capacidade instalada deve estar entre 0 e 3.000 kWp, limite da minigeração " +
                    "distribuída para fonte solar.");
        }
        if (centroDAO.buscarPorId(centroId).isEmpty()) {
            throw new RegraNegocioException("Centro de distribuição inválido.");
        }
        Optional<UnidadeConsumidora> mesmoNumero = unidadeDAO.buscarPorNumero(numero);
        if (mesmoNumero.isPresent() && mesmoNumero.get().getId() != u.getId()) {
            throw new RegraNegocioException("Já existe uma unidade cadastrada com o número " + numero + ".");
        }

        u.setCentroDistribuicaoId(centroId);
        u.setNumeroUc(numero);
        u.setEndereco(end);
        u.setCapacidadeGeracaoKwp(capacidadeKwp);
    }

    private static String plural(int quantidade, String singular, String plural) {
        return quantidade + " " + (quantidade == 1 ? singular : plural);
    }
}
