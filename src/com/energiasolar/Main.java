package com.energiasolar;

import com.energiasolar.dao.ConexaoBD;
import com.energiasolar.dao.UsuarioDAO;
import com.energiasolar.model.CreditoEnergia;
import com.energiasolar.model.RegistroEnergia;
import com.energiasolar.model.Usuario;
import com.energiasolar.model.UnidadeConsumidora;
import com.energiasolar.service.AutenticacaoService;
import com.energiasolar.service.EnergiaService;
import com.energiasolar.service.MarketplaceService;
import com.energiasolar.dao.UnidadeConsumidoraDAO;
import com.energiasolar.web.ServidorWeb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class Main {

    private static final int PORTA_PADRAO = 8080;
    private static final String SCHEMA = "database/schema.sql";

    public static void main(String[] args) throws IOException {
        Locale.setDefault(new Locale("pt", "BR"));
        System.out.println("=== Marketplace de Energia Solar (ODS 9) ===");

        if (Files.notExists(Path.of(SCHEMA))) {
            System.err.println("ERRO: arquivo " + SCHEMA + " nao encontrado.");
            System.err.println("Execute o sistema a partir da pasta raiz do projeto (a que contem as pastas database e webapp).");
            System.exit(1);
        }

        int porta = lerPorta(args);

        ConexaoBD.inicializarBanco(SCHEMA);
        executarScript("database/seed.sql");

        seedDadosDemo();

        try {
            ServidorWeb.iniciar(porta, "webapp");
        } catch (java.net.BindException e) {
            System.err.println("ERRO: a porta " + porta + " ja esta em uso por outro programa.");
            System.err.println("Execute informando outra porta, por exemplo: java -jar marketplace-energia-solar.jar 8081");
            System.exit(1);
        }
        System.out.println("Acesse: http://localhost:" + porta + "/login");
    }

    private static int lerPorta(String[] args) {
        if (args.length == 0) return PORTA_PADRAO;
        try {
            int porta = Integer.parseInt(args[0]);
            if (porta < 1024 || porta > 65535) throw new NumberFormatException();
            return porta;
        } catch (NumberFormatException e) {
            System.err.println("Porta invalida: " + args[0] + ". Usando a porta padrao " + PORTA_PADRAO + ".");
            return PORTA_PADRAO;
        }
    }

    private static void executarScript(String caminho) {
        try (var con = ConexaoBD.obterConexao(); var stmt = con.createStatement()) {
            String sql = Files.readString(Path.of(caminho));
            for (String comando : sql.split(";")) {
                String cmd = comando.trim();
                if (!cmd.isEmpty()) stmt.execute(cmd);
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao executar script " + caminho, e);
        }
    }

    private static void seedDadosDemo() {
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        if (!usuarioDAO.listarTodos().isEmpty()) {
            System.out.println("[SEED] Banco já populado, pulando dados de demonstração.");
            return;
        }

        System.out.println("[SEED] Criando usuários e dados de demonstração...");
        AutenticacaoService autenticacaoService = new AutenticacaoService();
        UnidadeConsumidoraDAO unidadeDAO = new UnidadeConsumidoraDAO();
        EnergiaService energiaService = new EnergiaService();
        MarketplaceService marketplaceService = new MarketplaceService();

        Usuario ana = autenticacaoService.cadastrar("Ana Souza", "ana@exemplo.com", "123456", "GERADOR", "Maringá", "PR");
        Usuario joao = autenticacaoService.cadastrar("João Pereira", "joao@exemplo.com", "123456", "CONSUMIDOR", "Maringá", "PR");
        Usuario carla = autenticacaoService.cadastrar("Carla Lima", "carla@exemplo.com", "123456", "AMBOS", "Sarandi", "PR");

        UnidadeConsumidora ucAna = novaUnidade(unidadeDAO, ana.getId(), 1, "UC-1001", "Rua das Palmeiras, 120 - Maringá/PR", 5.4);
        UnidadeConsumidora ucJoao = novaUnidade(unidadeDAO, joao.getId(), 1, "UC-2001", "Av. Brasil, 900 - Maringá/PR", 0);
        UnidadeConsumidora ucCarla = novaUnidade(unidadeDAO, carla.getId(), 1, "UC-1002", "Rua Sarandi, 45 - Sarandi/PR", 8.0);

        String mesAtual = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        RegistroEnergia registroAna = energiaService.lancarRegistro(ucAna.getId(), mesAtual, 620.0, 380.0);

        energiaService.lancarRegistro(ucCarla.getId(), mesAtual, 900.0, 500.0);

        energiaService.lancarRegistro(ucJoao.getId(), mesAtual, 0.0, 350.0);

        List<CreditoEnergia> creditosAna = energiaService.creditosDisponiveis(ucAna.getId());
        if (!creditosAna.isEmpty()) {
            CreditoEnergia credito = creditosAna.get(0);
            double quantidadeOfertada = Math.min(100.0, credito.getQuantidadeDisponivelKwh());
            marketplaceService.publicarOferta(credito.getId(), ana.getId(), quantidadeOfertada, 0.68);
        }

        System.out.println("[SEED] Concluído. Usuários de teste (senha 123456): ana@exemplo.com, joao@exemplo.com, carla@exemplo.com");
    }

    private static UnidadeConsumidora novaUnidade(UnidadeConsumidoraDAO dao, int usuarioId, int centroId,
                                                    String numeroUc, String endereco, double capacidade) {
        UnidadeConsumidora u = new UnidadeConsumidora();
        u.setUsuarioId(usuarioId);
        u.setCentroDistribuicaoId(centroId);
        u.setNumeroUc(numeroUc);
        u.setEndereco(endereco);
        u.setCapacidadeGeracaoKwp(capacidade);
        return dao.inserir(u);
    }
}
