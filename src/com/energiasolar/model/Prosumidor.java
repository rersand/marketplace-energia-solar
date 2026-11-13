package com.energiasolar.model;

/**
 * Participante que gera e consome energia. Pode vender o excedente em um mês e comprar
 * crédito em outro, conforme o saldo da sua geração, acumulando as duas permissões.
 */
public class Prosumidor extends Usuario {

    @Override
    public String getTipo() {
        return "AMBOS";
    }

    @Override
    public String rotuloPerfil() {
        return "Gerador e Consumidor";
    }

    @Override
    public boolean podeOfertar() {
        return true;
    }

    @Override
    public boolean podeComprar() {
        return true;
    }
}
