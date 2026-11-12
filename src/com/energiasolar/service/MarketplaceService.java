package com.energiasolar.service;

import com.energiasolar.dao.ConexaoBD;
import com.energiasolar.dao.CreditoEnergiaDAO;
import com.energiasolar.dao.OfertaVendaDAO;
import com.energiasolar.dao.TransacaoDAO;
import com.energiasolar.dao.UnidadeConsumidoraDAO;
import com.energiasolar.dao.UsuarioDAO;
import com.energiasolar.model.CreditoEnergia;
import com.energiasolar.model.OfertaVenda;
import com.energiasolar.model.Transacao;
import com.energiasolar.model.UnidadeConsumidora;
import com.energiasolar.model.Usuario;

import java.util.List;

/**
 * Regras de negócio do marketplace: publicação, manutenção (CRUD) e compra de ofertas.
 *
 * Antes de cada operação consulta as permissões do participante por meio de
 * podeOfertar() e podeComprar(), declarados como abstratos em Usuario e implementados
 * por cada subclasse.
 *
 * Toda operação que movimenta saldo de energia é executada em uma única transação:
 * ou todas as gravações são confirmadas, ou nenhuma é, de modo que o saldo de um crédito
 * e o saldo da oferta lastreada nele nunca fiquem inconsistentes entre si.
 */
public class MarketplaceService {

    private static final double TOLERANCIA_KWH = 0.0001;

    private final CreditoEnergiaDAO creditoDAO = new CreditoEnergiaDAO();
    private final OfertaVendaDAO ofertaDAO = new OfertaVendaDAO();
    private final TransacaoDAO transacaoDAO = new TransacaoDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final UnidadeConsumidoraDAO unidadeDAO = new UnidadeConsumidoraDAO();

    /**
     * Publica uma oferta a partir de um crédito do próprio vendedor. A oferta é negociada
     * no centro de distribuição que atende a unidade geradora do crédito.
     */
    public OfertaVenda publicarOferta(int creditoId, int vendedorId, double quantidadeKwh, double precoKwh) {
        Usuario vendedor = buscarUsuario(vendedorId);
        if (!vendedor.podeOfertar()) {
            throw new RegraNegocioException(
                    "O perfil " + vendedor.rotuloPerfil() + " não pode publicar ofertas de venda.");
        }

        CreditoEnergia credito = creditoDAO.buscarPorId(creditoId)
                .orElseThrow(() -> new RegraNegocioException("Crédito de energia não encontrado."));
        UnidadeConsumidora unidade = unidadeDAO.buscarPorId(credito.getUnidadeId())
                .orElseThrow(() -> new RegraNegocioException("Crédito de energia não encontrado."));
        if (unidade.getUsuarioId() != vendedorId) {
            throw new RegraNegocioException("Crédito de energia não encontrado.");
        }

        if (!"DISPONIVEL".equals(credito.getStatus()) && !"PARCIAL".equals(credito.getStatus())) {
            throw new RegraNegocioException("Este crédito não está mais disponível.");
        }
        if (quantidadeKwh <= 0 || quantidadeKwh > credito.getQuantidadeDisponivelKwh() + TOLERANCIA_KWH) {
            throw new RegraNegocioException("A quantidade ofertada deve ser maior que zero e não pode exceder " +
                    "o saldo disponível do crédito (%.2f kWh).".formatted(credito.getQuantidadeDisponivelKwh()));
        }
        validarPreco(precoKwh);

        return ConexaoBD.emTransacao(() -> {
            double novoSaldoCredito = credito.getQuantidadeDisponivelKwh() - quantidadeKwh;
            creditoDAO.atualizarSaldo(creditoId, Math.max(0, novoSaldoCredito),
                    novoSaldoCredito <= TOLERANCIA_KWH ? "ESGOTADO" : "PARCIAL");

            OfertaVenda oferta = new OfertaVenda();
            oferta.setCreditoId(creditoId);
            oferta.setVendedorId(vendedorId);
            oferta.setCentroDistribuicaoId(unidade.getCentroDistribuicaoId());
            oferta.setQuantidadeKwh(quantidadeKwh);
            oferta.setQuantidadeDisponivelKwh(quantidadeKwh);
            oferta.setPrecoKwh(precoKwh);
            return ofertaDAO.inserir(oferta);
        });
    }

    public List<OfertaVenda> ofertasDisponiveisParaComprador(int centroId, int usuarioId) {
        return ofertaDAO.listarAbertasPorCentro(centroId, usuarioId);
    }

    /** Todas as ofertas abertas de um centro, inclusive as do próprio usuário (exibidas na vitrine como "Sua oferta"). */
    public List<OfertaVenda> vitrineDoCentro(int centroId) {
        return ofertaDAO.listarAbertasPorCentro(centroId);
    }

    public List<OfertaVenda> minhasOfertas(int vendedorId) {
        return ofertaDAO.listarPorVendedor(vendedorId);
    }

    /** Altera o preço por kWh de uma oferta ainda aberta. */
    public OfertaVenda editarPreco(int ofertaId, int vendedorId, double novoPreco) {
        OfertaVenda oferta = ofertaDoVendedor(ofertaId, vendedorId);
        if (!"ABERTA".equals(oferta.getStatus())) {
            throw new RegraNegocioException("Só é possível alterar o preço de ofertas abertas.");
        }
        validarPreco(novoPreco);
        ofertaDAO.atualizarPreco(ofertaId, novoPreco);
        oferta.setPrecoKwh(novoPreco);
        return oferta;
    }

    /**
     * Encerra uma oferta aberta. O saldo que ainda não foi vendido é devolvido ao crédito
     * de origem, voltando a ficar disponível para uma nova oferta.
     */
    public double cancelarOferta(int ofertaId, int vendedorId) {
        OfertaVenda oferta = ofertaDoVendedor(ofertaId, vendedorId);
        if (!"ABERTA".equals(oferta.getStatus())) {
            throw new RegraNegocioException("Esta oferta não está aberta e não pode ser cancelada.");
        }
        return ConexaoBD.emTransacao(() -> {
            double devolvido = devolverSaldoAoCredito(oferta);
            ofertaDAO.atualizarSaldo(ofertaId, 0, "CANCELADA");
            return devolvido;
        });
    }

    /**
     * Remove uma oferta do sistema. Ofertas com vendas registradas não podem ser removidas,
     * pois as transações são o registro financeiro de compradores e vendedores: nesse caso
     * a operação é recusada e o usuário é orientado a cancelar a oferta.
     */
    public double excluirOferta(int ofertaId, int vendedorId) {
        OfertaVenda oferta = ofertaDoVendedor(ofertaId, vendedorId);
        int vendas = transacaoDAO.contarPorOferta(ofertaId);
        if (vendas > 0) {
            throw new RegraNegocioException(
                    "A oferta não pode ser excluída: possui " + vendas + (vendas == 1 ? " venda registrada" : " vendas registradas") +
                    ". O histórico de transações é um registro financeiro e precisa ser preservado. " +
                    ("ABERTA".equals(oferta.getStatus())
                            ? "Para tirá-la do marketplace, use Cancelar."
                            : "A oferta já está encerrada e permanece apenas como histórico."));
        }
        return ConexaoBD.emTransacao(() -> {
            double devolvido = devolverSaldoAoCredito(oferta);
            ofertaDAO.excluirPorId(ofertaId);
            return devolvido;
        });
    }

    /** Compra total ou parcial de uma oferta aberta do centro de distribuição do comprador. */
    public Transacao comprar(int ofertaId, int compradorId, double quantidadeKwh) {
        Usuario comprador = buscarUsuario(compradorId);
        if (!comprador.podeComprar()) {
            throw new RegraNegocioException(
                    "O perfil " + comprador.rotuloPerfil() + " não pode comprar energia.");
        }

        OfertaVenda oferta = ofertaDAO.buscarPorId(ofertaId)
                .orElseThrow(() -> new RegraNegocioException("Oferta não encontrada."));
        if (!"ABERTA".equals(oferta.getStatus())) {
            throw new RegraNegocioException("Esta oferta não está mais aberta para compra.");
        }
        if (oferta.getVendedorId() == compradorId) {
            throw new RegraNegocioException("Você não pode comprar a própria oferta.");
        }
        if (!unidadeDAO.existeNoCentro(compradorId, oferta.getCentroDistribuicaoId())) {
            throw new RegraNegocioException("A negociação de energia só é permitida entre unidades atendidas " +
                    "pelo mesmo centro de distribuição, e você não possui unidade no centro desta oferta.");
        }
        if (quantidadeKwh <= 0 || quantidadeKwh > oferta.getQuantidadeDisponivelKwh() + TOLERANCIA_KWH) {
            throw new RegraNegocioException("A quantidade deve ser maior que zero e não pode exceder o disponível " +
                    "na oferta (%.2f kWh).".formatted(oferta.getQuantidadeDisponivelKwh()));
        }

        return ConexaoBD.emTransacao(() -> {
            Transacao t = new Transacao();
            t.setOfertaId(ofertaId);
            t.setCompradorId(compradorId);
            t.setQuantidadeKwh(quantidadeKwh);
            t.setValorTotal(quantidadeKwh * oferta.getPrecoKwh());
            t = transacaoDAO.inserir(t);

            double novoSaldo = oferta.getQuantidadeDisponivelKwh() - quantidadeKwh;
            ofertaDAO.atualizarSaldo(ofertaId, Math.max(0, novoSaldo),
                    novoSaldo <= TOLERANCIA_KWH ? "FECHADA" : "ABERTA");
            return t;
        });
    }

    private double devolverSaldoAoCredito(OfertaVenda oferta) {
        double devolvido = oferta.getQuantidadeDisponivelKwh();
        if (devolvido <= TOLERANCIA_KWH) {
            return 0;
        }
        CreditoEnergia credito = creditoDAO.buscarPorId(oferta.getCreditoId())
                .orElseThrow(() -> new RegraNegocioException("Crédito de origem da oferta não encontrado."));
        double novoSaldo = Math.min(credito.getQuantidadeKwh(), credito.getQuantidadeDisponivelKwh() + devolvido);
        String novoStatus = novoSaldo >= credito.getQuantidadeKwh() - TOLERANCIA_KWH ? "DISPONIVEL" : "PARCIAL";
        creditoDAO.atualizarSaldo(credito.getId(), novoSaldo, novoStatus);
        return devolvido;
    }

    private OfertaVenda ofertaDoVendedor(int ofertaId, int vendedorId) {
        OfertaVenda oferta = ofertaDAO.buscarPorId(ofertaId)
                .orElseThrow(() -> new RegraNegocioException("Oferta não encontrada."));
        if (oferta.getVendedorId() != vendedorId) {
            throw new RegraNegocioException("Oferta não encontrada.");
        }
        return oferta;
    }

    private Usuario buscarUsuario(int usuarioId) {
        return usuarioDAO.buscarPorId(usuarioId)
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));
    }

    private void validarPreco(double precoKwh) {
        if (precoKwh <= 0 || precoKwh > 10) {
            throw new RegraNegocioException("O preço por kWh deve ser maior que R$ 0,00 e no máximo R$ 10,00.");
        }
    }
}
