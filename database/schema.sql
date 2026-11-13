-- =====================================================================
-- MARKETPLACE DE ENERGIA SOLAR - ODS 9
-- Modelo DER (Diagrama de Entidade-Relacionamento) implementado em SQL
--
-- Compatível com SQLite (usado no runtime da aplicação).
-- Para migrar para MySQL/PostgreSQL, ver notas de compatibilidade no README.
-- =====================================================================

PRAGMA foreign_keys = ON;

-- ---------------------------------------------------------------------
-- USUARIO: pessoas cadastradas na plataforma (geradores, consumidores,
-- ou ambos - "prosumidores", e administradores do sistema).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuario (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    nome            TEXT NOT NULL,
    email           TEXT NOT NULL UNIQUE,
    senha_hash      TEXT NOT NULL,
    senha_salt      TEXT NOT NULL,
    tipo            TEXT NOT NULL CHECK (tipo IN ('GERADOR', 'CONSUMIDOR', 'AMBOS', 'ADMIN')),
    cidade          TEXT,
    uf              TEXT,
    data_cadastro   TEXT NOT NULL DEFAULT (datetime('now'))
);

-- ---------------------------------------------------------------------
-- CENTRO_DISTRIBUICAO: nós regionais que agregam a energia excedente
-- de uma região e fazem o repasse/roteamento entre unidades. Não
-- armazenam energia fisicamente - representam a área de rede/
-- distribuidora que faz o balanço de créditos daquela região
-- (equivalente a uma zona de atendimento de uma distribuidora).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS centro_distribuicao (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    nome            TEXT NOT NULL,
    cidade          TEXT NOT NULL,
    uf              TEXT NOT NULL,
    capacidade_mwh  REAL NOT NULL DEFAULT 0
);

-- ---------------------------------------------------------------------
-- UNIDADE_CONSUMIDORA: cada ponto de conexão à rede (uma casa ou
-- empresa) pertence a um usuário e está vinculada a um centro de
-- distribuição, que é quem intermedia suas trocas de energia.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS unidade_consumidora (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id              INTEGER NOT NULL,
    centro_distribuicao_id  INTEGER NOT NULL,
    numero_uc               TEXT NOT NULL UNIQUE,
    endereco                TEXT NOT NULL,
    capacidade_geracao_kwp  REAL NOT NULL DEFAULT 0,
    FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE,
    FOREIGN KEY (centro_distribuicao_id) REFERENCES centro_distribuicao(id)
);

-- ---------------------------------------------------------------------
-- REGISTRO_ENERGIA: leitura periódica (mensal) de geração e consumo
-- de uma unidade. O excedente é calculado a partir daqui.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS registro_energia (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    unidade_id          INTEGER NOT NULL,
    mes_referencia      TEXT NOT NULL,            -- formato 'YYYY-MM'
    energia_gerada_kwh  REAL NOT NULL DEFAULT 0,
    energia_consumida_kwh REAL NOT NULL DEFAULT 0,
    excedente_kwh       REAL NOT NULL DEFAULT 0,   -- calculado no service
    data_registro       TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (unidade_id) REFERENCES unidade_consumidora(id) ON DELETE CASCADE,
    UNIQUE (unidade_id, mes_referencia)
);

-- ---------------------------------------------------------------------
-- CREDITO_ENERGIA: crédito de energia (kWh) gerado quando há
-- excedente num registro. Tem validade (padrão: 60 meses, como na
-- Lei 14.300/2022) e um saldo disponível que diminui à medida que é
-- vendido no marketplace.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS credito_energia (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    unidade_id              INTEGER NOT NULL,
    registro_id             INTEGER NOT NULL UNIQUE,
    quantidade_kwh          REAL NOT NULL,
    quantidade_disponivel_kwh REAL NOT NULL,
    data_geracao            TEXT NOT NULL DEFAULT (datetime('now')),
    data_validade           TEXT NOT NULL,
    status                  TEXT NOT NULL DEFAULT 'DISPONIVEL'
                            CHECK (status IN ('DISPONIVEL', 'PARCIAL', 'ESGOTADO', 'EXPIRADO')),
    FOREIGN KEY (unidade_id) REFERENCES unidade_consumidora(id) ON DELETE CASCADE,
    FOREIGN KEY (registro_id) REFERENCES registro_energia(id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- OFERTA_VENDA: um vendedor (usuário GERADOR/AMBOS) coloca parte ou
-- todo o seu crédito disponível à venda no marketplace do seu centro
-- de distribuição.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS oferta_venda (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    credito_id              INTEGER NOT NULL,
    vendedor_id             INTEGER NOT NULL,
    centro_distribuicao_id  INTEGER NOT NULL,
    quantidade_kwh          REAL NOT NULL,
    quantidade_disponivel_kwh REAL NOT NULL,
    preco_kwh               REAL NOT NULL,
    status                  TEXT NOT NULL DEFAULT 'ABERTA'
                            CHECK (status IN ('ABERTA', 'FECHADA', 'CANCELADA')),
    data_criacao            TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (credito_id) REFERENCES credito_energia(id),
    FOREIGN KEY (vendedor_id) REFERENCES usuario(id),
    FOREIGN KEY (centro_distribuicao_id) REFERENCES centro_distribuicao(id)
);

-- ---------------------------------------------------------------------
-- TRANSACAO: compra (parcial ou total) de uma oferta por outro
-- usuário. Uma oferta pode ter várias transações até esgotar.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transacao (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    oferta_id       INTEGER NOT NULL,
    comprador_id    INTEGER NOT NULL,
    quantidade_kwh  REAL NOT NULL,
    valor_total     REAL NOT NULL,
    data_transacao  TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (oferta_id) REFERENCES oferta_venda(id),
    FOREIGN KEY (comprador_id) REFERENCES usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_oferta_status ON oferta_venda(status, centro_distribuicao_id);
CREATE INDEX IF NOT EXISTS idx_credito_unidade ON credito_energia(unidade_id, status);
CREATE INDEX IF NOT EXISTS idx_registro_unidade ON registro_energia(unidade_id);
