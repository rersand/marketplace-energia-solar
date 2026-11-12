package com.energiasolar.service;

import com.energiasolar.dao.ConexaoBD;
import com.energiasolar.dao.CreditoEnergiaDAO;
import com.energiasolar.dao.RegistroEnergiaDAO;
import com.energiasolar.model.CreditoEnergia;
import com.energiasolar.model.RegistroEnergia;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Regras de apuração de energia e geração de crédito.
 *
 * Reproduz a lógica do Sistema de Compensação de Energia Elétrica (Lei 14.300/2022): a
 * energia injetada na rede converte-se em crédito, com validade de 60 meses.
 *
 * O crédito nunca é criado isoladamente — gerarCredito() é privado e só é acionado por
 * lancarRegistro(), de modo que todo crédito decorre de um lançamento mensal que apurou
 * excedente real.
 */
public class EnergiaService {

    private static final int VALIDADE_CREDITO_MESES = 60;

    /** Geração máxima de uma usina de 3 MW operando 24 horas por dia durante 31 dias. */
    private static final double LIMITE_MENSAL_KWH = 2_232_000;

    private final RegistroEnergiaDAO registroDAO = new RegistroEnergiaDAO();
    private final CreditoEnergiaDAO creditoDAO = new CreditoEnergiaDAO();

    public RegistroEnergia lancarRegistro(int unidadeId, String mesReferencia,
                                           double energiaGeradaKwh, double energiaConsumidaKwh) {
        validarMes(mesReferencia);
        if (energiaGeradaKwh < 0 || energiaConsumidaKwh < 0) {
            throw new RegraNegocioException("Os valores de energia gerada e consumida não podem ser negativos.");
        }
        if (energiaGeradaKwh > LIMITE_MENSAL_KWH || energiaConsumidaKwh > LIMITE_MENSAL_KWH) {
            throw new RegraNegocioException("Valor de energia acima do limite aceito para um mês (2.232.000 kWh).");
        }
        if (registroDAO.existeNoMes(unidadeId, mesReferencia)) {
            throw new RegraNegocioException("O mês " + mesReferencia + " já foi lançado para esta unidade.");
        }

        double excedente = Math.max(0, energiaGeradaKwh - energiaConsumidaKwh);

        return ConexaoBD.emTransacao(() -> {
            RegistroEnergia registro = new RegistroEnergia();
            registro.setUnidadeId(unidadeId);
            registro.setMesReferencia(mesReferencia);
            registro.setEnergiaGeradaKwh(energiaGeradaKwh);
            registro.setEnergiaConsumidaKwh(energiaConsumidaKwh);
            registro.setExcedenteKwh(excedente);
            registro = registroDAO.inserir(registro);

            if (excedente > 0) {
                gerarCredito(unidadeId, registro.getId(), excedente);
            }
            return registro;
        });
    }

    private void validarMes(String mesReferencia) {
        YearMonth mes;
        try {
            mes = YearMonth.parse(mesReferencia == null ? "" : mesReferencia);
        } catch (DateTimeParseException e) {
            throw new RegraNegocioException("Mês de referência inválido. Use o formato AAAA-MM.");
        }
        if (mes.isAfter(YearMonth.now())) {
            throw new RegraNegocioException("Não é possível lançar energia de um mês futuro: " + mesReferencia + ".");
        }
    }

    private CreditoEnergia gerarCredito(int unidadeId, int registroId, double quantidadeKwh) {
        CreditoEnergia credito = new CreditoEnergia();
        credito.setUnidadeId(unidadeId);
        credito.setRegistroId(registroId);
        credito.setQuantidadeKwh(quantidadeKwh);
        credito.setQuantidadeDisponivelKwh(quantidadeKwh);
        credito.setStatus("DISPONIVEL");

        LocalDate validade = LocalDate.now().plusMonths(VALIDADE_CREDITO_MESES);
        credito.setDataValidade(validade.format(DateTimeFormatter.ISO_LOCAL_DATE));

        return creditoDAO.inserir(credito);
    }

    public List<RegistroEnergia> historicoDaUnidade(int unidadeId) {
        return registroDAO.listarPorUnidade(unidadeId);
    }

    public List<CreditoEnergia> creditosDisponiveis(int unidadeId) {
        return creditoDAO.listarDisponiveisPorUnidade(unidadeId);
    }

    public List<CreditoEnergia> historicoCreditos(int unidadeId) {
        return creditoDAO.listarPorUnidade(unidadeId);
    }

    public double saldoTotalDisponivel(int unidadeId) {
        return creditosDisponiveis(unidadeId).stream()
                .mapToDouble(CreditoEnergia::getQuantidadeDisponivelKwh)
                .sum();
    }
}
