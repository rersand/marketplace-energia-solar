package com.energiasolar.model;

public class OfertaVenda {
    private int id;
    private int creditoId;
    private int vendedorId;
    private int centroDistribuicaoId;
    private double quantidadeKwh;
    private double quantidadeDisponivelKwh;
    private double precoKwh;
    private String status;
    private String dataCriacao;

    private String nomeVendedor;
    private String nomeCentro;

    public OfertaVenda() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCreditoId() { return creditoId; }
    public void setCreditoId(int creditoId) { this.creditoId = creditoId; }

    public int getVendedorId() { return vendedorId; }
    public void setVendedorId(int vendedorId) { this.vendedorId = vendedorId; }

    public int getCentroDistribuicaoId() { return centroDistribuicaoId; }
    public void setCentroDistribuicaoId(int centroDistribuicaoId) { this.centroDistribuicaoId = centroDistribuicaoId; }

    public double getQuantidadeKwh() { return quantidadeKwh; }
    public void setQuantidadeKwh(double quantidadeKwh) { this.quantidadeKwh = quantidadeKwh; }

    public double getQuantidadeDisponivelKwh() { return quantidadeDisponivelKwh; }
    public void setQuantidadeDisponivelKwh(double quantidadeDisponivelKwh) { this.quantidadeDisponivelKwh = quantidadeDisponivelKwh; }

    public double getPrecoKwh() { return precoKwh; }
    public void setPrecoKwh(double precoKwh) { this.precoKwh = precoKwh; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(String dataCriacao) { this.dataCriacao = dataCriacao; }

    public String getNomeVendedor() { return nomeVendedor; }
    public void setNomeVendedor(String nomeVendedor) { this.nomeVendedor = nomeVendedor; }

    public String getNomeCentro() { return nomeCentro; }
    public void setNomeCentro(String nomeCentro) { this.nomeCentro = nomeCentro; }
}
