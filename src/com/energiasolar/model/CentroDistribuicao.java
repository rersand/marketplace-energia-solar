package com.energiasolar.model;

public class CentroDistribuicao {
    private int id;
    private String nome;
    private String cidade;
    private String uf;
    private double capacidadeMwh;

    public CentroDistribuicao() {}

    public CentroDistribuicao(int id, String nome, String cidade, String uf, double capacidadeMwh) {
        this.id = id;
        this.nome = nome;
        this.cidade = cidade;
        this.uf = uf;
        this.capacidadeMwh = capacidadeMwh;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }

    public double getCapacidadeMwh() { return capacidadeMwh; }
    public void setCapacidadeMwh(double capacidadeMwh) { this.capacidadeMwh = capacidadeMwh; }
}
