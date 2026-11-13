package com.energiasolar.model;

public class UnidadeConsumidora {
    private int id;
    private int usuarioId;
    private int centroDistribuicaoId;
    private String numeroUc;
    private String endereco;
    private double capacidadeGeracaoKwp;

    private String nomeCentro;

    public UnidadeConsumidora() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public int getCentroDistribuicaoId() { return centroDistribuicaoId; }
    public void setCentroDistribuicaoId(int centroDistribuicaoId) { this.centroDistribuicaoId = centroDistribuicaoId; }

    public String getNumeroUc() { return numeroUc; }
    public void setNumeroUc(String numeroUc) { this.numeroUc = numeroUc; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public double getCapacidadeGeracaoKwp() { return capacidadeGeracaoKwp; }
    public void setCapacidadeGeracaoKwp(double capacidadeGeracaoKwp) { this.capacidadeGeracaoKwp = capacidadeGeracaoKwp; }

    public String getNomeCentro() { return nomeCentro; }
    public void setNomeCentro(String nomeCentro) { this.nomeCentro = nomeCentro; }
}
