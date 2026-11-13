package com.energiasolar.model;

public class CreditoEnergia {
    private int id;
    private int unidadeId;
    private int registroId;
    private double quantidadeKwh;
    private double quantidadeDisponivelKwh;
    private String dataValidade;
    private String status;

    public CreditoEnergia() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUnidadeId() { return unidadeId; }
    public void setUnidadeId(int unidadeId) { this.unidadeId = unidadeId; }

    public int getRegistroId() { return registroId; }
    public void setRegistroId(int registroId) { this.registroId = registroId; }

    public double getQuantidadeKwh() { return quantidadeKwh; }
    public void setQuantidadeKwh(double quantidadeKwh) { this.quantidadeKwh = quantidadeKwh; }

    public double getQuantidadeDisponivelKwh() { return quantidadeDisponivelKwh; }
    public void setQuantidadeDisponivelKwh(double quantidadeDisponivelKwh) { this.quantidadeDisponivelKwh = quantidadeDisponivelKwh; }

    public String getDataValidade() { return dataValidade; }
    public void setDataValidade(String dataValidade) { this.dataValidade = dataValidade; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
