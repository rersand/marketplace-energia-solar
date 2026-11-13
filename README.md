# Marketplace de Energia Solar — ODS 9

Projeto acadêmico (ODS 9 — Indústria, Inovação e Infraestrutura): marketplace
para a comercialização de energia solar excedente entre residências e empresas,
utilizando o modelo de **crédito de energia** — o mesmo princípio do Sistema de
Compensação de Energia Elétrica (SCEE) instituído pela Lei nº 14.300/2022 —,
com centros de distribuição regionais intermediando as negociações.

**Stack:** Java (servidor HTTP embutido do JDK, sem frameworks externos) + HTML/CSS + SQL (SQLite).

---

## Sumário

- [Escopo funcional e ODS](#escopo-funcional-e-ods)
- [Lista de Requisitos Funcionais](#lista-de-requisitos-funcionais)
- [Cronograma e backlog de execução](#cronograma-e-backlog-de-execução)
- [Justificativa técnica](#justificativa-técnica)
- [Herança e polimorfismo](#herança-e-polimorfismo)
- [CRUD e regras de exclusão](#crud-e-regras-de-exclusão)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Como executar](#como-executar)
- [Documentação completa](#documentação-completa)

---

## Escopo funcional e ODS

O sistema resolve três dores identificadas na pesquisa de mercado sobre geração
distribuída no Brasil:

1. **O crédito de energia perde valor parado.** A tarifação do componente Fio B
   subiu de 15% (2023) para 60% (2026), com previsão de 100% em 2029; além
   disso, o crédito expira em 60 meses sem qualquer ressarcimento ao consumidor.
   Hoje o titular não tem mecanismo para negociá-lo ativamente.
2. **Falta de canal de negociação.** O SCEE é um sistema passivo de compensação:
   o gerador não escolhe destino nem preço do seu excedente.
3. **Exclusão de quem não pode gerar.** Quem mora de aluguel, em apartamento ou
   sem telhado adequado fica fora dos benefícios da energia solar própria.

**Alinhamento ao ODS 9** — o projeto atende diretamente às metas:

- **Meta 9.1** (infraestrutura com acesso equitativo e a preços acessíveis):
  amplia o acesso à energia limpa a consumidores sem geração própria, sem
  investimento inicial em equipamentos.
- **Meta 9.4** (modernizar a infraestrutura com foco em recursos renováveis):
  propõe uma camada de software que aproveita melhor a infraestrutura solar já
  instalada, reduzindo o desperdício de créditos que hoje expiram sem uso.

---

## Lista de Requisitos Funcionais

| Código | Requisito |
|---|---|
| **RF01** | O sistema deve permitir o cadastro de usuários, com nome completo, e-mail, senha, tipo de participação (gerador, consumidor ou ambos), cidade e unidade federativa. |
| **RF02** | O sistema deve permitir a autenticação do usuário por e-mail e senha, mantendo a sessão ativa e restringindo o acesso às funcionalidades apenas a usuários autenticados. |
| **RF03** | O sistema deve permitir o cadastro de unidades consumidoras vinculadas ao usuário, com número da unidade consumidora, endereço, centro de distribuição de atendimento e capacidade de geração instalada em kWp. |
| **RF04** | O sistema deve permitir o lançamento periódico da energia gerada e da energia consumida por uma unidade consumidora, informando o mês de referência, em kWh. |
| **RF05** | O sistema deve calcular automaticamente o excedente de energia de cada lançamento e, havendo excedente, gerar automaticamente um crédito de energia correspondente, com validade de 60 meses, conforme a Lei nº 14.300/2022. |
| **RF06** | O sistema deve permitir ao usuário consultar o saldo de créditos de energia disponíveis por unidade consumidora, com a respectiva data de validade de cada crédito. |
| **RF07** | O sistema deve permitir ao usuário gerador publicar uma oferta de venda a partir de um crédito disponível, informando a quantidade em kWh e o preço por kWh livremente definido por ele. |
| **RF08** | O sistema deve exibir ao usuário comprador apenas as ofertas abertas pertencentes ao mesmo centro de distribuição de suas unidades consumidoras, ordenadas por preço, excluindo as ofertas publicadas por ele próprio. |
| **RF09** | O sistema deve permitir a compra total ou parcial de uma oferta disponível, registrando a transação com quantidade e valor total e atualizando automaticamente o saldo restante da oferta. |
| **RF10** | O sistema deve gerar relatórios de acompanhamento, contendo o extrato individual de compras e vendas do usuário e o total de energia negociada e faturada agregado por centro de distribuição. |
| **RF11** | O sistema deve permitir ao usuário editar e excluir as suas unidades consumidoras, recusando a exclusão, com a indicação do motivo, quando existirem ofertas no marketplace lastreadas nos créditos da unidade. |
| **RF12** | O sistema deve permitir ao vendedor alterar o preço de uma oferta aberta, cancelá-la — devolvendo ao crédito de origem o saldo não vendido — e excluí-la, recusando a exclusão, com a indicação do motivo, quando a oferta possuir vendas registradas. |

---

## Cronograma e backlog de execução

Cada item corresponde a uma entrega concreta, identificada pelo requisito funcional
que atende quando aplicável, com o integrante responsável e a situação atual.
Os itens RF11, RF12 e B08 a B12 correspondem à 2ª Entrega.

| Período | ID | Item do backlog | Responsável | Status |
|---|---|---|---|---|
| 04/08 a 31/08/2026 | B01 | Descoberta: pesquisa de mercado e levantamento das dores dos usuários de geração distribuída | Caio Sobrinho | Concluído |
| 04/08 a 31/08/2026 | B02 | Modelagem do banco de dados e elaboração do DER | Gabriel Pierini | Concluído |
| 04/08 a 31/08/2026 | B03 | Definição da arquitetura em camadas e das tecnologias do projeto | Gabriel Pierini | Concluído |
| 01/09 a 28/09/2026 | RF01, RF02 | Cadastro e autenticação de usuários com senha protegida por hash e salt | André Ruan da Silva | Concluído |
| 01/09 a 28/09/2026 | RF03 | Cadastro de unidades consumidoras vinculadas ao centro de distribuição | André Ruan da Silva | Concluído |
| 01/09 a 28/09/2026 | RF04, RF05 | Lançamento mensal de geração e consumo com cálculo automático do excedente e geração do crédito | Gabriel Pierini | Concluído |
| 01/09 a 28/09/2026 | RF06 | Consulta do saldo de créditos disponíveis por unidade consumidora | Caio Sobrinho | Concluído |
| 29/09 a 19/10/2026 | RF07 | Publicação de oferta de venda a partir de crédito disponível | Gabriel Pierini | Concluído |
| 29/09 a 19/10/2026 | RF08 | Listagem das ofertas abertas do mesmo centro de distribuição | Caio Sobrinho | Concluído |
| 29/09 a 19/10/2026 | RF09 | Compra total ou parcial de oferta com atualização automática do saldo | Gabriel Pierini | Concluído |
| 29/09 a 19/10/2026 | RF10 | Relatórios por centro de distribuição e extratos de compra e venda | André Ruan da Silva | Concluído |
| 20/10 a 11/11/2026 | B04 | Refatoração orientada a objetos: herança e polimorfismo (`Usuario` abstrata e `AbstractDAO`) | Gabriel Pierini | Concluído |
| 20/10 a 11/11/2026 | B05 | Documentação, diagramas UML e organização do repositório no GitHub | Caio Sobrinho | Concluído |
| 20/10 a 11/11/2026 | RF11 | CRUD completo de unidades consumidoras, com bloqueio motivado da exclusão | André Ruan da Silva | Concluído |
| 20/10 a 11/11/2026 | RF12 | CRUD completo de ofertas: edição de preço, cancelamento com estorno e exclusão | Gabriel Pierini | Concluído |
| 20/10 a 11/11/2026 | B08 | Validação de formulários, mensagens de erro legíveis e proteção contra injeção de HTML | Caio Sobrinho | Concluído |
| 20/10 a 11/11/2026 | B09 | Manutenção do SQLite embarcado, transações atômicas e jar executável multiplataforma | Gabriel Pierini | Concluído |
| 20/10 a 11/11/2026 | B10 | Interface por perfil: vitrine no Início, menu lateral com área de Vendas (resumo, ofertas e estoque) e cor de destaque conforme `podeComprar()` e `podeOfertar()` | Gabriel Pierini | Concluído |
| 12/11 a 30/11/2026 | B11 | Teste de execução do pacote final em Windows e macOS | André Ruan da Silva | A fazer |
| 12/11 a 30/11/2026 | B12 | Preparação e apresentação da 2ª Entrega | Caio Sobrinho | A fazer |

### Equipe

Gabriel Pierini &middot; André Ruan da Silva &middot; Caio Sobrinho

---

## Justificativa técnica

| Escolha | Justificativa resumida |
|---|---|
| **Java** | Tipagem estática favorece a confiabilidade dos requisitos de cálculo de saldo e valor (RF05, RF09); portabilidade via JVM; o próprio JDK fornece servidor HTTP (`com.sun.net.httpserver`), atendendo RF02/RF08/RF10 sem frameworks externos. |
| **Banco relacional (SQL)** | Os relacionamentos exigidos por RF03–RF09 são estáveis e bem definidos; chaves estrangeiras e restrições de integridade impedem estados inconsistentes (ex.: oferta maior que o crédito disponível, ou saldo negativo após compra parcial). |
| **Arquitetura em camadas** | As regras de negócio (RF05, RF07, RF09) ficam isoladas na camada `service`, separadas da interface e do banco — testáveis em um único lugar e reaproveitáveis caso o sistema exponha as mesmas funcionalidades por outro canal (API REST, app móvel). |

A defesa completa dessas escolhas — incluindo a discussão sobre por que **não** se
adotou uma arquitetura descentralizada baseada em blockchain — está na seção 3 do
documento em [`/docs`](./docs).

---

## Herança e polimorfismo

O sistema usa herança em dois pontos, cada um resolvendo um problema real:

### 1. Perfis de participante (domínio)

`Usuario` é uma **classe abstrata**. Não existe participante genérico — todo
usuário é um `Gerador`, um `Consumidor` ou um `Prosumidor`, e o que os distingue
são as permissões de atuação no marketplace:

| Subclasse | `podeOfertar()` | `podeComprar()` |
|---|---|---|
| `Gerador` | `true` | `false` |
| `Consumidor` | `false` | `true` |
| `Prosumidor` | `true` | `true` |

O **polimorfismo** aparece no `MarketplaceService`: antes de publicar uma oferta
ou registrar uma compra, o serviço chama `usuario.podeOfertar()` ou
`usuario.podeComprar()` sem saber qual subclasse recebeu a chamada. A mesma
decisão também adapta a interface: o `Layout` monta o menu lateral chamando os
mesmos métodos, então cada perfil só enxerga as áreas que pode usar.

### Navegação

Depois do login, todos caem no **Início**: a vitrine com as ofertas abertas do
centro de distribuição das suas unidades. O botão **☰ Menu**, à esquerda do topo,
abre o painel lateral:

| Item do menu | Gerador | Consumidor | Prosumidor (Ambos) |
|---|:-:|:-:|:-:|
| Início (vitrine) | referência de preço | compra | compra |
| Minhas compras | | ✓ | ✓ |
| **Vendas ›** Resumo · Minhas ofertas · Estoque de créditos | ✓ | | ✓ |
| **Minha energia ›** Unidades consumidoras · Registrar energia | ✓ | ✓ (Registrar consumo) | ✓ |
| Relatórios | ✓ | ✓ | ✓ |
| Cor de destaque | Amarelo | Azul | Verde |

- **Vendas** é a área do vendedor, com submenu próprio: o **Resumo** mostra
  estoque, ofertas abertas e últimas vendas; **Minhas ofertas** concentra a
  publicação e o CRUD das ofertas; o **Estoque de créditos** lista cada crédito
  gerado (mês de origem, saldo, validade e situação), com o atalho *Anunciar*.
- Na vitrine, a oferta do próprio usuário aparece marcada como *Sua oferta*, com
  atalho para Vendas; quem só vende vê as ofertas como referência de preço.
- Quem tenta abrir uma área que não é do seu perfil é redirecionado para a área certa.

No banco continua existindo uma única tabela `usuario` (*Single Table
Inheritance*): a coluna `tipo` é o discriminador, e o método `Usuario.novoPerfil()`
instancia a subclasse correta na leitura (`UsuarioDAO.mapear()`) e no cadastro.

### 2. Acesso a dados (arquitetura)

`AbstractDAO<T>` é a superclasse abstrata dos sete DAOs, aplicando o padrão
**Template Method**: ela implementa o esqueleto das operações (abrir conexão,
preparar comando, percorrer o `ResultSet`, tratar erro, fechar recursos) e deixa
duas decisões para a subclasse — `nomeTabela()` e `mapear(rs)`, ambas abstratas.

Quando `listarTodos()` da superclasse chama `mapear()`, quem decide o que será
produzido é a subclasse concreta em tempo de execução: o mesmo código devolve um
`Usuario`, um `CreditoEnergia` ou uma `Transacao`. Subclasses também
**sobrescrevem** métodos concretos quando precisam — `UsuarioDAO` troca a
ordenação padrão e `OfertaVendaDAO` redefine `buscarPorId()` para incluir JOINs.

Diagramas: `docs/Diagrama-de-Classes-Dominio.pdf` (herança de domínio) e
`docs/Diagrama-de-Classes-UML-Resumido.pdf` (as duas heranças, por camada).

---

## CRUD e regras de exclusão

O CRUD opera sobre as duas entidades centrais do escopo: **unidade consumidora**
(a âncora do domínio — tudo pende dela) e **oferta de venda** (o coração do
marketplace). Toda operação sobre um registro existente confere antes que ele
pertence ao usuário logado.

| Operação | Unidade consumidora (RF03, RF11) | Oferta de venda (RF07, RF12) |
|---|---|---|
| **Create** | Cadastro com validação de número único, endereço e capacidade | Publicação a partir de crédito do próprio vendedor |
| **Read** | Listagem do usuário e formulário de edição preenchido | Vitrine no Início, "Minhas ofertas" e "Estoque de créditos" em Vendas |
| **Update** | Edição de todos os dados; troca de centro bloqueada se houver oferta aberta | Alteração do preço; cancelamento devolve o saldo não vendido ao crédito |
| **Delete** | Exclusão física em cascata (lançamentos e créditos), **bloqueada com o motivo** se houver ofertas | Exclusão física com estorno do saldo, **bloqueada com o motivo** se houver vendas |

**Por que a exclusão pode ser recusada.** A tabela `oferta_venda` referencia
`credito_energia` sem `ON DELETE CASCADE`, e isso é proposital: apagar uma unidade
apagaria os créditos dela e, com eles, as ofertas e as transações — o registro
financeiro de outros usuários. Por isso a regra é verificada no `UnidadeService`
**antes** do `DELETE`, e o usuário recebe uma mensagem que explica o motivo e diz
o que fazer, em vez de um erro de banco:

> A unidade UC-1001 não pode ser excluída: possui 1 oferta publicada no marketplace
> (1 aberta), lastreada(s) nos créditos dela. Exclua essas ofertas em Vendas ›
> Minhas ofertas e tente novamente.

Pelo mesmo motivo, uma oferta com vendas registradas não pode ser excluída — apenas
cancelada, o que a retira do marketplace e preserva o histórico.

**Atomicidade.** Operações que movimentam saldo de energia (publicar, comprar,
cancelar, excluir oferta e lançar energia) rodam em uma única transação via
`ConexaoBD.emTransacao()`: se qualquer etapa falhar, nada é gravado, e o saldo do
crédito nunca diverge do saldo da oferta lastreada nele.

---

## Estrutura do projeto

```
energia-solar/
├── src/                        # código-fonte Java
│   └── com/energiasolar/
│       ├── Inicializador.java  # entrada do jar: confere a versão do Java
│       ├── Main.java           # inicialização do banco e do servidor
│       ├── model/              # entidades (Usuario abstrata + Gerador/Consumidor/Prosumidor)
│       ├── dao/                # acesso a dados (AbstractDAO<T> + 7 DAOs concretas)
│       ├── service/            # regras de negócio
│       └── web/                # servidor HTTP e páginas
├── database/                   # scripts SQL
│   ├── schema.sql              # o Modelo DER, em DDL
│   └── seed.sql                # dados iniciais
├── docs/                       # documentação acadêmica (DOCX e PDF)
├── webapp/style.css            # estilos da interface
├── lib/                        # driver JDBC do SQLite (Windows, Linux e macOS)
├── marketplace-energia-solar.jar   # sistema compilado, pronto para executar
├── executar.bat / executar.sh  # executa o sistema
└── compilar.bat / compilar.sh  # recompila o jar a partir do código-fonte
```

---

## Como executar

### Pré-requisito: Java 17 ou superior

É a **única** dependência a instalar. O banco de dados (SQLite) é embarcado: não há
servidor, usuário ou senha para configurar, e o driver em `lib/` já traz as
bibliotecas nativas para Windows, Linux e macOS (Intel e Apple Silicon).

Para conferir a versão instalada, abra um terminal e digite `java -version`.
Se o comando não for reconhecido ou a versão for menor que 17, instale o
**JDK 21 LTS** em <https://adoptium.net> (na instalação do Windows, marque a opção
*Set JAVA_HOME* / *Add to PATH*). Se o sistema for aberto com uma versão antiga,
ele mesmo exibe essa orientação em vez de um erro técnico.

### Windows

Dê dois cliques em **`executar.bat`** — ou, no Prompt de Comando, dentro da pasta do projeto:

```bat
executar.bat
```

### Linux e macOS

No terminal, dentro da pasta do projeto:

```bash
sh executar.sh
```

### Em qualquer sistema, sem os scripts

```bash
java -jar marketplace-energia-solar.jar
```

O comando deve ser executado **na pasta raiz do projeto**, a que contém as pastas
`database` e `webapp`.

Com o servidor no ar, acesse **http://localhost:8080/login** no navegador.
Para encerrar, feche a janela do terminal ou pressione `Ctrl + C`.

Na primeira execução o sistema cria o banco (`energiasolar.db`) e popula dados de
demonstração. Para recomeçar do zero, apague esse arquivo com o sistema parado.

### Problemas comuns

| Mensagem | Solução |
|---|---|
| `'java' não é reconhecido como um comando` | O Java não está instalado ou não está no PATH. Instale o JDK 21 LTS (ver acima). |
| `este sistema exige Java 17 ou superior` | Instale o JDK 21 LTS e abra um novo terminal. |
| `a porta 8080 ja esta em uso` | Execute em outra porta: `executar.bat 8081`, `sh executar.sh 8081` ou `java -jar marketplace-energia-solar.jar 8081`, e acesse `http://localhost:8081/login`. |
| `arquivo database/schema.sql nao encontrado` | O comando foi executado fora da pasta do projeto. Use os scripts, que já entram na pasta certa. |

### Recompilar a partir do código-fonte

Requer o JDK (não apenas o JRE). Gera um novo `marketplace-energia-solar.jar`:

```bash
compilar.bat        # Windows
sh compilar.sh      # Linux e macOS
```

### No IntelliJ IDEA

1. Abra a pasta `energia-solar` como projeto (**File → Open**).
2. Em **File → Project Structure → Project**, selecione um SDK 17 ou superior.
3. Marque `src` como *Sources Root* (botão direito → *Mark Directory as* → *Sources Root*).
4. Adicione `lib/sqlite-jdbc-3.46.1.3.jar` ao classpath:
   **File → Project Structure → Modules → Dependencies → + → JARs or Directories**.
5. Execute a classe `com.energiasolar.Main`, com o diretório de trabalho na raiz do projeto
   (é o padrão do IntelliJ).

### Usuários de teste (senha `123456`)

| E-mail | Perfil |
|---|---|
| ana@exemplo.com | Geradora — já tem crédito e uma oferta ativa no marketplace |
| joao@exemplo.com | Consumidor — pode comprar energia |
| carla@exemplo.com | Ambos (gera e consome) |

---

## Migrando de SQLite para MySQL/PostgreSQL

O SQLite foi mantido de propósito: dispensa servidor e garante que o sistema execute
em qualquer máquina sem configuração. Como o projeto usa JDBC puro, a troca, se
necessária, é direta:

1. Coloque o driver do seu banco (ex.: `mysql-connector-j`) em `lib/` e inclua-o no
   `Class-Path` dos scripts de compilação.
2. Em `src/com/energiasolar/dao/ConexaoBD.java`, altere a constante `URL_BANCO`.
3. Ajuste diferenças de sintaxe em `database/schema.sql` (`AUTOINCREMENT` →
   `AUTO_INCREMENT`, `TEXT`/`REAL` → `VARCHAR`/`DECIMAL`).

O restante do código não muda — é tudo JDBC padrão.

---

## Documentação completa

A documentação acadêmica do projeto (Descoberta, Concepção e Alinhamento aos ODS,
Justificativa Técnica e Arquitetural, Cronograma e Referências em ABNT) está na
pasta [`/docs`](./docs).
