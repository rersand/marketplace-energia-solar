package com.energiasolar.model;

/**
 * Participante sem geração própria. Compra crédito de energia de geradores da sua região.
 */
public class Consumidor extends Usuario {

    @Override
    public String getTipo() {
        return "CONSUMIDOR";
    }

    @Override
    public String rotuloPerfil() {
        return "Consumidor";
    }

    @Override
    public boolean podeOfertar() {
        return false;
    }

    @Override
    public boolean podeComprar() {
        return true;
    }
}
