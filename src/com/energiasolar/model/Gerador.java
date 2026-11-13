package com.energiasolar.model;

/**
 * Participante com geração própria. Vende o crédito de energia excedente que acumula
 * e não compra energia de terceiros.
 */
public class Gerador extends Usuario {

    @Override
    public String getTipo() {
        return "GERADOR";
    }

    @Override
    public String rotuloPerfil() {
        return "Gerador";
    }

    @Override
    public boolean podeOfertar() {
        return true;
    }

    @Override
    public boolean podeComprar() {
        return false;
    }
}
