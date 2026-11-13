package com.energiasolar.model;

/**
 * Representa um participante do marketplace.
 *
 * Classe abstrata: não existe participante genérico no sistema — todo usuário é um
 * {@link Gerador}, um {@link Consumidor} ou um {@link Prosumidor}. Esta classe concentra
 * apenas o que os três têm em comum (identificação, credenciais e localização).
 *
 * O que difere entre os perfis são as permissões de atuação, declaradas aqui como métodos
 * abstratos e implementadas por cada subclasse. As regras de negócio invocam esses métodos
 * sem conhecer a subclasse concreta que recebeu a chamada, caracterizando o polimorfismo.
 */
public abstract class Usuario {

    private int id;
    private String nome;
    private String email;
    private String senhaHash;
    private String senhaSalt;
    private String cidade;
    private String uf;

    protected Usuario() {
    }

    /** Código persistido na coluna 'tipo', definido pela subclasse. */
    public abstract String getTipo();

    /** Rótulo legível do perfil, exibido na interface. */
    public abstract String rotuloPerfil();

    /** Indica se este perfil pode publicar ofertas de venda de energia. */
    public abstract boolean podeOfertar();

    /** Indica se este perfil pode comprar energia de outros participantes. */
    public abstract boolean podeComprar();

    /**
     * Instancia a subclasse correspondente ao código de tipo.
     * Utilizada no cadastro e na leitura do banco, concentrando em um único ponto a
     * decisão sobre qual perfil criar.
     */
    public static Usuario novoPerfil(String tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo de participante não informado");
        }
        return switch (tipo.toUpperCase()) {
            case "GERADOR" -> new Gerador();
            case "CONSUMIDOR" -> new Consumidor();
            case "AMBOS" -> new Prosumidor();
            default -> throw new IllegalArgumentException("Tipo de participante desconhecido: " + tipo);
        };
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }

    public String getSenhaSalt() { return senhaSalt; }
    public void setSenhaSalt(String senhaSalt) { this.senhaSalt = senhaSalt; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
}
