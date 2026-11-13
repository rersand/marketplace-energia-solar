-- Dados de exemplo para demonstração
-- Senha de todos os usuários de teste: "123456"
-- (hash gerado pelo próprio sistema no primeiro start, ver Main.java -> seedDados())

INSERT OR IGNORE INTO centro_distribuicao (id, nome, cidade, uf, capacidade_mwh)
VALUES
    (1, 'Centro de Distribuição Maringá Norte', 'Maringá', 'PR', 500.0),
    (2, 'Centro de Distribuição Sarandi', 'Sarandi', 'PR', 200.0);
