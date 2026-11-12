package com.energiasolar.service;

/**
 * Violação de uma regra de negócio do marketplace.
 *
 * Diferencia as operações recusadas por uma regra do domínio — cuja mensagem é escrita
 * para o usuário e exibida na interface — das falhas técnicas inesperadas, que não devem
 * ter seus detalhes expostos na tela.
 */
public class RegraNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
