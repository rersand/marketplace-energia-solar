package com.energiasolar.model;

public class Transacao {
    private int id;
    private int ofertaId;
    private int compradorId;
    private double quantidadeKwh;
    private double valorTotal;
    private String dataTransacao;

    private String nomeComprador;
    private String nomeVendedor;

    public Transacao() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOfertaId() { return ofertaId; }
    public void setOfertaId(int ofertaId) { this.ofertaId = ofertaId; }

    public int getCompradorId() { return compradorId; }
    public void setCompradorId(int compradorId) { this.compradorId = compradorId; }

    public double getQuantidadeKwh() { return quantidadeKwh; }
    public void setQuantidadeKwh(double quantidadeKwh) { this.quantidadeKwh = quantidadeKwh; }

    public double getValorTotal() { return valorTotal; }
    public void setValorTotal(double valorTotal) { this.valorTotal = valorTotal; }

    public String getDataTransacao() { return dataTransacao; }
    public void setDataTransacao(String dataTransacao) { this.dataTransacao = dataTransacao; }

    public String getNomeComprador() { return nomeComprador; }
    public void setNomeComprador(String nomeComprador) { this.nomeComprador = nomeComprador; }

    public String getNomeVendedor() { return nomeVendedor; }
    public void setNomeVendedor(String nomeVendedor) { this.nomeVendedor = nomeVendedor; }
}
