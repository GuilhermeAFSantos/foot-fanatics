package br.senac.footfanatics.treino.jdbc;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.senac.footfanatics.infra.CredenciaisDosPapeis;
import br.senac.footfanatics.infra.ProvisionadorDeBanco;
import br.senac.footfanatics.treino.ExecucaoDeTreino;
import br.senac.footfanatics.treino.StatusExecucao;
import br.senac.footfanatics.treino.StatusVersao;
import br.senac.footfanatics.treino.VersaoDoModelo;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Regras que só existem no banco real: unicidade, advisory lock, transação única, imutabilidade e papéis
 * (docs/arquitetura.md §3, §6.3; ADR-009, ADR-010, ADR-015). Requer Docker.
 * Nomes de tabela seguem as entidades de §3 em minúsculas.
 */
@Testcontainers
class PersistenciaDoTreinoIntegracaoTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));

    private static final CredenciaisDosPapeis SENHAS =
            new CredenciaisDosPapeis("senha-api-teste", "senha-leitor-teste", "senha-registry-teste");
    private static final LocalDate DIA = LocalDate.of(2026, 11, 13);
    private static final Instant W0 = Instant.parse("2026-11-12T12:00:00Z");

    private DataSource admin;

    private static DataSource dataSource(String usuario, String senha, String aplicacao) {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(POSTGRES.getJdbcUrl());
        ds.setUser(usuario);
        ds.setPassword(senha);
        ds.setApplicationName(aplicacao);
        return ds;
    }

    @BeforeEach
    void provisionar() throws SQLException {
        admin = dataSource(POSTGRES.getUsername(), POSTGRES.getPassword(), "admin-teste");
        new ProvisionadorDeBanco().provisionar(admin, SENHAS);
        isolarDoTesteAnterior();
    }

    /**
     * Isolamento (decisão registrada no plano, §7): o contêiner é compartilhado pela classe, então cada teste
     * começa com o schema ml vazio e sem conexões de testes anteriores segurando o advisory lock.
     * Só prepara o ambiente; não altera nenhuma asserção.
     */
    private void isolarDoTesteAnterior() throws SQLException {
        try (Connection con = admin.getConnection(); Statement st = con.createStatement()) {
            st.execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity"
                    + " WHERE datname = current_database() AND pid <> pg_backend_pid()");
            st.execute("TRUNCATE ml.ponteiro_modelo_ativo, ml.quarentena, ml.versao_modelo, ml.execucao_treino CASCADE");
            // O CASCADE acima também esvazia checkpoint_treino (FK ultima_execucao_id): recria a linha do job
            // como o provisionamento a deixa. Sem isso watermark() voltava null e comparava null com null.
            st.execute("INSERT INTO ml.checkpoint_treino (job, watermark) VALUES ('treino-diario', 'epoch')"
                    + " ON CONFLICT (job) DO UPDATE SET watermark = 'epoch', ultima_execucao_id = NULL");
        }
    }

    private static String sqlStateAoExecutar(String usuario, String senha, String sql) {
        try (Connection con = DriverManager.getConnection(POSTGRES.getJdbcUrl(), usuario, senha);
             Statement st = con.createStatement()) {
            st.execute(sql);
            return "OK";
        } catch (SQLException e) {
            return e.getSQLState();
        }
    }

    private static final String SEM_PRIVILEGIO = "42501";

    // ---------------- RS-03 ----------------

    @Test
    @DisplayName("RS-03 · CT-S03.03 · nunca existem duas execuções com a mesma data_referencia")
    void rs03_cts03_03_dataReferenciaUnica() {
        RegistryDeModelosJdbc registry = new RegistryDeModelosJdbc(admin);
        registry.salvarExecucao(new ExecucaoDeTreino(UUID.randomUUID(), DIA, StatusExecucao.EM_EXECUCAO, W0, W0, "h"));

        try {
            registry.salvarExecucao(new ExecucaoDeTreino(UUID.randomUUID(), DIA, StatusExecucao.EM_EXECUCAO, W0, W0, "h"));
        } catch (RuntimeException rejeitada) {
            // rejeição é o comportamento esperado; a contagem abaixo é a verificação
        }

        assertEquals(1, registry.execucoes().stream().filter(e -> e.dataReferencia().equals(DIA)).count());
    }

    // ---------------- RS-05 ----------------

    @Test
    @DisplayName("RS-05 · CT-S05.02 · queda do processo dono do lock libera o lock")
    void rs05_cts05_02_quedaLiberaLock() throws SQLException {
        GerenciadorDeLockPostgres primeiro = new GerenciadorDeLockPostgres(dataSource(
                POSTGRES.getUsername(), POSTGRES.getPassword(), "job-que-cai"));
        assertTrue(primeiro.tentarAdquirir());

        try (Connection con = admin.getConnection(); Statement st = con.createStatement()) {
            st.execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE application_name = 'job-que-cai'");
        }

        GerenciadorDeLockPostgres segundo = new GerenciadorDeLockPostgres(dataSource(
                POSTGRES.getUsername(), POSTGRES.getPassword(), "job-seguinte"));
        assertTrue(segundo.tentarAdquirir());
    }

    @Test
    @DisplayName("RS-05 · CT-S05.03 · nunca duas execuções com o lock ao mesmo tempo")
    void rs05_cts05_03_umLockPorVez() {
        GerenciadorDeLockPostgres a = new GerenciadorDeLockPostgres(dataSource(
                POSTGRES.getUsername(), POSTGRES.getPassword(), "job-a"));
        GerenciadorDeLockPostgres b = new GerenciadorDeLockPostgres(dataSource(
                POSTGRES.getUsername(), POSTGRES.getPassword(), "job-b"));

        boolean primeiro = a.tentarAdquirir();
        boolean segundo = b.tentarAdquirir();

        assertTrue(primeiro);
        assertFalse(segundo);
    }

    // ---------------- RS-10 ----------------

    @Test
    @DisplayName("RS-10 · CT-S10.02 · falha na transação final não altera ponteiro, execução nem checkpoint")
    void rs10_cts10_02_transacaoFinalTudoOuNada() {
        RegistryDeModelosJdbc registry = new RegistryDeModelosJdbc(admin);
        ExecucaoDeTreino e = new ExecucaoDeTreino(UUID.randomUUID(), DIA, StatusExecucao.EM_EXECUCAO, W0, W0.plusSeconds(60), "h");
        registry.salvarExecucao(e);
        Instant watermarkAntes = registry.watermark();
        Optional<UUID> ativaAntes = registry.versaoAtiva();

        // versão promovida inexistente força a falha dentro da transação final
        try {
            registry.finalizar(e.id(), StatusExecucao.CONCLUIDA, e.corte(), UUID.randomUUID());
        } catch (RuntimeException esperada) {
            // a verificação é o estado abaixo
        }

        assertAll(
                () -> assertEquals(watermarkAntes, registry.watermark()),
                () -> assertEquals(ativaAntes, registry.versaoAtiva()),
                () -> assertEquals(StatusExecucao.EM_EXECUCAO, registry.buscarExecucao(DIA).orElseThrow().status()));
    }

    // ---------------- RS-11 ----------------

    @Test
    @DisplayName("RS-11 · CT-S11.03 · o papel da API não altera o ponteiro do modelo ativo")
    void rs11_cts11_03_apiNaoAlteraPonteiro() {
        assertEquals(SEM_PRIVILEGIO, sqlStateAoExecutar("api_app", SENHAS.senhaApiApp(),
                "UPDATE ml.ponteiro_modelo_ativo SET alterado_em = now()"));
    }

    // ---------------- RS-12 ----------------

    @Test
    @DisplayName("RS-12 · CT-S12.01 · versão registrada é imutável (hash, parâmetros, execução)")
    void rs12_cts12_01_versaoImutavel() {
        RegistryDeModelosJdbc registry = new RegistryDeModelosJdbc(admin);
        UUID exec = UUID.randomUUID();
        registry.salvarExecucao(new ExecucaoDeTreino(exec, DIA, StatusExecucao.EM_EXECUCAO, W0, W0, "h"));
        UUID id = UUID.randomUUID();
        registry.registrarVersao(new VersaoDoModelo(id, StatusVersao.CANDIDATO, exec, null, "hash-original", "c", "p"));

        try {
            registry.registrarVersao(new VersaoDoModelo(id, StatusVersao.CANDIDATO, exec, null, "hash-adulterado", "c", "p"));
        } catch (RuntimeException rejeitada) {
            // rejeição é o comportamento esperado
        }

        assertEquals("hash-original", registry.versoes().stream()
                .filter(v -> v.id().equals(id)).findFirst().orElseThrow().hashArtefato());
    }

    @Test
    @Disabled("Bloqueado: L-11, tecnologia do Armazenamento de Artefatos adiada (ADR-013)")
    @DisplayName("RS-12 · CT-S12.02 · artefato gravado nunca é sobrescrito")
    void rs12_cts12_02_artefatoNaoSobrescrito() {
    }

    // ---------------- RS-15 ----------------

    @Test
    @DisplayName("RS-15 · CT-S15.01 · batch_leitor não lê o schema app")
    void rs15_cts15_01_batchLeitorSemAcessoAApp() {
        assertEquals(SEM_PRIVILEGIO, sqlStateAoExecutar("batch_leitor", SENHAS.senhaBatchLeitor(),
                "SELECT 1 FROM app.usuario"));
    }

    @Test
    @DisplayName("RS-15 · CT-S15.02 · batch_leitor lê o schema eventos")
    void rs15_cts15_02_batchLeitorLeEventos() {
        assertEquals("OK", sqlStateAoExecutar("batch_leitor", SENHAS.senhaBatchLeitor(),
                "SELECT count(*) FROM eventos.evento_de_entrada"));
    }

    @Test
    @DisplayName("RS-15 · CT-S15.03 · api_app não escreve no schema ml")
    void rs15_cts15_03_apiNaoEscreveEmMl() {
        assertEquals(SEM_PRIVILEGIO, sqlStateAoExecutar("api_app", SENHAS.senhaApiApp(),
                "DELETE FROM ml.execucao_treino"));
    }

    // ---------------- Aula 07: issue do SonarCloud (java:S2077) ----------------

    @Test
    @DisplayName("RS-15 · CT-S15.04 · senha com $$, aspas e comentário provisiona o papel sem injetar SQL")
    void rs15_cts15_04_senhaHostilNaoInjetaSql() {
        String hostil = "x$$'; DROP SCHEMA ml CASCADE; -- \\ \"";
        CredenciaisDosPapeis credenciais = new CredenciaisDosPapeis(hostil, SENHAS.senhaBatchLeitor(), SENHAS.senhaBatchRegistry());

        new ProvisionadorDeBanco().provisionar(admin, credenciais);

        assertAll(
                () -> assertEquals("OK", sqlStateAoExecutar("api_app", hostil, "SELECT count(*) FROM app.usuario")),
                () -> assertEquals("OK", sqlStateAoExecutar(POSTGRES.getUsername(), POSTGRES.getPassword(),
                        "SELECT 1 FROM ml.checkpoint_treino")));
    }

    // ---------------- Aula 07: lacunas de risco apontadas pelo JaCoCo ----------------

    private static final Instant CORTE_1 = W0.plusSeconds(3600);
    private static final Instant CORTE_2 = W0.plusSeconds(7200);

    private UUID execucaoComCandidato(RegistryDeModelosJdbc registry, LocalDate dia, Instant inicio, Instant corte,
                                      UUID base, String hash) {
        UUID exec = UUID.randomUUID();
        registry.salvarExecucao(new ExecucaoDeTreino(exec, dia, StatusExecucao.EM_EXECUCAO, inicio, corte, "h"));
        registry.registrarVersao(new VersaoDoModelo(UUID.randomUUID(), StatusVersao.CANDIDATO, exec, base, hash, "c", "p"));
        return exec;
    }

    private static UUID versaoDa(RegistryDeModelosJdbc registry, UUID exec) {
        return registry.versoes().stream().filter(v -> exec.equals(v.execucaoId())).findFirst().orElseThrow().id();
    }

    @Test
    @DisplayName("RS-05 · CT-S05.04 · liberar o lock permite a próxima execução; quem já tem o lock não o perde ao pedir de novo")
    void rs05_cts05_04_liberarPermiteAProxima() {
        GerenciadorDeLockPostgres dono = new GerenciadorDeLockPostgres(dataSource(
                POSTGRES.getUsername(), POSTGRES.getPassword(), "job-dono"));
        GerenciadorDeLockPostgres seguinte = new GerenciadorDeLockPostgres(dataSource(
                POSTGRES.getUsername(), POSTGRES.getPassword(), "job-seguinte"));
        assertTrue(dono.tentarAdquirir());

        boolean donoDeNovo = dono.tentarAdquirir();
        boolean seguinteAntes = seguinte.tentarAdquirir();
        dono.liberar();
        dono.liberar(); // liberar sem lock não tem efeito nem lança
        boolean seguinteDepois = seguinte.tentarAdquirir();

        assertAll(
                () -> assertTrue(donoDeNovo),
                () -> assertFalse(seguinteAntes),
                () -> assertTrue(seguinteDepois));
    }

    @Test
    @DisplayName("RS-10 · CT-S10.04 · no PostgreSQL a transação final aplica status, checkpoint e ponteiro (com a anterior) juntos")
    void rs10_cts10_04_transacaoFinalConfirmada() {
        RegistryDeModelosJdbc registry = new RegistryDeModelosJdbc(admin);
        UUID e1 = execucaoComCandidato(registry, DIA, W0, CORTE_1, null, "hash-1");
        UUID v1 = versaoDa(registry, e1);

        registry.finalizar(e1, StatusExecucao.CONCLUIDA, CORTE_1, v1);

        assertAll(
                () -> assertEquals(StatusExecucao.CONCLUIDA, registry.buscarExecucao(DIA).orElseThrow().status()),
                () -> assertEquals(CORTE_1, registry.watermark()),
                () -> assertEquals(Optional.of(v1), registry.versaoAtiva()),
                () -> assertEquals(Optional.empty(), registry.versaoAnterior()));

        UUID e2 = execucaoComCandidato(registry, DIA.plusDays(1), CORTE_1, CORTE_2, v1, "hash-2");
        UUID v2 = versaoDa(registry, e2);

        registry.finalizar(e2, StatusExecucao.CONCLUIDA, CORTE_2, v2);

        assertAll(
                () -> assertEquals(CORTE_2, registry.watermark()),
                () -> assertEquals(Optional.of(v2), registry.versaoAtiva()),
                () -> assertEquals(Optional.of(v1), registry.versaoAnterior()));
    }

    @Test
    @DisplayName("RS-02 · CT-S02.06 · no PostgreSQL a finalização REPROVADA muda o status sem mover checkpoint nem ponteiro")
    void rs02_cts02_06_reprovadaNaoMoveCheckpointNoBanco() {
        RegistryDeModelosJdbc registry = new RegistryDeModelosJdbc(admin);
        UUID e1 = execucaoComCandidato(registry, DIA, W0, CORTE_1, null, "hash-1");
        registry.finalizar(e1, StatusExecucao.CONCLUIDA, CORTE_1, versaoDa(registry, e1));
        Optional<UUID> ativaAntes = registry.versaoAtiva();
        UUID e2 = execucaoComCandidato(registry, DIA.plusDays(1), CORTE_1, CORTE_2, ativaAntes.orElseThrow(), "hash-2");

        registry.finalizar(e2, StatusExecucao.REPROVADA, null, null);

        assertAll(
                () -> assertEquals(StatusExecucao.REPROVADA, registry.buscarExecucao(DIA.plusDays(1)).orElseThrow().status()),
                () -> assertEquals(CORTE_1, registry.watermark()),
                () -> assertEquals(ativaAntes, registry.versaoAtiva()));
    }

    @Test
    @DisplayName("RS-11 · CT-S11.08 · no PostgreSQL a troca manual do ponteiro e a mudança de status persistem")
    void rs11_cts11_08_trocaManualPersistida() {
        RegistryDeModelosJdbc registry = new RegistryDeModelosJdbc(admin);
        UUID e1 = execucaoComCandidato(registry, DIA, W0, CORTE_1, null, "hash-1");
        UUID v1 = versaoDa(registry, e1);
        registry.finalizar(e1, StatusExecucao.CONCLUIDA, CORTE_1, v1);
        UUID e2 = execucaoComCandidato(registry, DIA.plusDays(1), CORTE_1, CORTE_2, v1, "hash-2");
        UUID v2 = versaoDa(registry, e2);
        registry.finalizar(e2, StatusExecucao.CONCLUIDA, CORTE_2, v2);

        registry.atualizarStatusDaVersao(v2, StatusVersao.APROVADO_INATIVO);
        registry.atualizarStatusDaVersao(v1, StatusVersao.ATIVO);
        registry.trocarPonteiro(v1, v2);

        assertAll(
                () -> assertEquals(Optional.of(v1), registry.versaoAtiva()),
                () -> assertEquals(Optional.of(v2), registry.versaoAnterior()),
                () -> assertEquals(CORTE_2, registry.watermark()),
                () -> assertEquals(StatusVersao.ATIVO, registry.versoes().stream()
                        .filter(v -> v.id().equals(v1)).findFirst().orElseThrow().status()),
                () -> assertEquals(StatusVersao.APROVADO_INATIVO, registry.versoes().stream()
                        .filter(v -> v.id().equals(v2)).findFirst().orElseThrow().status()));
    }
}
