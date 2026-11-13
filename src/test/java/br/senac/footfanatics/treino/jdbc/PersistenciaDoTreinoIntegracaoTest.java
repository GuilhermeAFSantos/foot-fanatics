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
    void provisionar() {
        admin = dataSource(POSTGRES.getUsername(), POSTGRES.getPassword(), "admin-teste");
        new ProvisionadorDeBanco().provisionar(admin, SENHAS);
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
}
