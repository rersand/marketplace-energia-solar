package com.energiasolar.model;

public class RegistroEnergia {
    private int id;
    private int unidadeId;
    private String mesReferencia;
    private double energiaGeradaKwh;
    private double energiaConsumidaKwh;
    private double excedenteKwh;

    public RegistroEnergia() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUnidadeId() { return unidadeId; }
    public void setUnidadeId(int unidadeId) { this.unidadeId = unidadeId; }

    public String getMesReferencia() { return mesReferencia; }
    public void setMesReferencia(String mesReferencia) { this.mesReferencia = mesReferencia; }

    public double getEnergiaGeradaKwh() { return energiaGeradaKwh; }
    public void setEnergiaGeradaKwh(double energiaGeradaKwh) { this.energiaGeradaKwh = energiaGeradaKwh; }

    public double getEnergiaConsumidaKwh() { return energiaConsumidaKwh; }
    public void setEnergiaConsumidaKwh(double energiaConsumidaKwh) { this.energiaConsumidaKwh = energiaConsumidaKwh; }

    public double getExcedenteKwh() { return excedenteKwh; }
    public void setExcedenteKwh(double excedenteKwh) { this.excedenteKwh = excedenteKwh; }
}
