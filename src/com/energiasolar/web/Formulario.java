package com.energiasolar.web;

import com.energiasolar.service.RegraNegocioException;

import java.util.Map;

/**
 * Leitura e validação dos campos enviados pelos formulários.
 *
 * Converte valores ausentes ou mal formatados em mensagens compreensíveis para o
 * usuário, em vez de deixar a conversão falhar com um erro técnico. Aceita vírgula ou
 * ponto como separador decimal.
 */
public class Formulario {

    private final Map<String, String> campos;

    public Formulario(Map<String, String> campos) {
        this.campos = campos;
    }

    public String texto(String campo, String rotulo) {
        String valor = campos.get(campo);
        if (valor == null || valor.isBlank()) {
            throw new RegraNegocioException("Preencha o campo \"" + rotulo + "\".");
        }
        return valor.trim();
    }

    public int inteiro(String campo, String rotulo) {
        try {
            return Integer.parseInt(texto(campo, rotulo));
        } catch (NumberFormatException e) {
            throw new RegraNegocioException("O campo \"" + rotulo + "\" deve ser um número inteiro.");
        }
    }

    public double decimal(String campo, String rotulo) {
        try {
            return Double.parseDouble(texto(campo, rotulo).replace(",", "."));
        } catch (NumberFormatException e) {
            throw new RegraNegocioException("O campo \"" + rotulo + "\" deve ser um número, por exemplo 12,5.");
        }
    }

    public double decimalOuZero(String campo, String rotulo) {
        String valor = campos.get(campo);
        return (valor == null || valor.isBlank()) ? 0 : decimal(campo, rotulo);
    }
}
