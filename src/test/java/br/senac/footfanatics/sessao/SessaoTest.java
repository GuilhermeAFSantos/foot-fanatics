package br.senac.footfanatics.sessao;

import static br.senac.footfanatics.apoio.ValoresDeTeste.TOLERANCIA_REUSO;
import static br.senac.footfanatics.apoio.ValoresDeTeste.TTL_ACESSO;
import static br.senac.footfanatics.apoio.ValoresDeTeste.TTL_REFRESH;
import static br.senac.footfanatics.apoio.ValoresDeTeste.UM_MS;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.senac.footfanatics.apoio.CenarioDeAcesso;
import br.senac.footfanatics.erro.CodigoDeErro;
import br.senac.footfanatics.erro.FalhaDeAcesso;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** E2 Sessão & Acesso: RN-05 a RN-09 (ADR-004, Proposta: casos condicionais). */
class SessaoTest {

    private static final String EMAIL = "bia@exemplo.test";
    private static final String SENHA = "senha-123";

    private CenarioDeAcesso c;
    private ServicoDeSessao sessao;
    private UUID usuario;

    @BeforeEach
    void montar() {
        c = new CenarioDeAcesso();
        sessao = c.sessao;
        usuario = c.autenticador.conhecer(EMAIL, SENHA);
    }

    private static CodigoDeErro codigoDaFalha(Runnable acao) {
        return assertThrows(FalhaDeAcesso.class, acao::run).codigo();
    }

    // ---------------- RN-05 Login (docs/arquitetura.md:617) ----------------

    @Test
    @DisplayName("RN-05 · CT-05.01 · login cria uma sessão e emite par; refresh só como hash")
    void rn05_ct05_01_loginCriaSessaoEEmitePar() {
        ParDeTokens par = sessao.login(EMAIL, SENHA, "celular");

        List<Sessao> sessoes = c.sessoes.listarTodas();
        assertAll(
                () -> assertNotNull(par.tokenDeAcesso()),
                () -> assertNotNull(par.refreshToken()),
                () -> assertNotEquals(par.tokenDeAcesso(), par.refreshToken()),
                () -> assertEquals(1, sessoes.size()),
                () -> assertEquals(usuario, sessoes.get(0).usuarioId()),
                () -> assertTrue(!sessoes.get(0).toString().contains(par.refreshToken())));
    }

    @Test
    @DisplayName("RN-05 · CT-05.02 · login em dois dispositivos cria duas sessões independentes")
    void rn05_ct05_02_doisDispositivosDuasSessoes() {
        ParDeTokens celular = sessao.login(EMAIL, SENHA, "celular");
        ParDeTokens computador = sessao.login(EMAIL, SENHA, "computador");

        assertAll(
                () -> assertEquals(2, c.sessoes.listarTodas().size()),
                () -> assertNotEquals(celular, computador),
                () -> assertEquals(usuario, sessao.validar(celular.tokenDeAcesso()).usuarioId()),
                () -> assertEquals(usuario, sessao.validar(computador.tokenDeAcesso()).usuarioId()));
    }

    @Test
    @DisplayName("RN-05 · CT-05.03 · credencial inválida não cria sessão")
    void rn05_ct05_03_credencialInvalidaNaoCriaSessao() {
        assertThrows(CredenciaisInvalidasException.class, () -> sessao.login(EMAIL, "errada", "celular"));

        assertTrue(c.sessoes.listarTodas().isEmpty());
    }

    @Test
    @Disabled("Bloqueado: QT-03 / AM-10, novo login no mesmo dispositivo substitui ou cria sessão?")
    @DisplayName("RN-05 · CT-05.04 · novo login no mesmo dispositivo")
    void rn05_ct05_04_novoLoginMesmoDispositivo() {
    }

    @Test
    @Disabled("Bloqueado: Q-05, limite de sessões não definido")
    @DisplayName("RN-05 · CT-05.05 · login além do limite de sessões")
    void rn05_ct05_05_limiteDeSessoes() {
    }

    // ---------------- RN-06 Validação do token (docs/arquitetura.md:618) ----------------

    @Test
    @DisplayName("RN-06 · CT-06.01 · token válido identifica o usuário")
    void rn06_ct06_01_tokenValidoIdentificaUsuario() {
        ParDeTokens par = sessao.login(EMAIL, SENHA, "celular");

        assertEquals(usuario, sessao.validar(par.tokenDeAcesso()).usuarioId());
    }

    @Test
    @DisplayName("RN-06 · CT-06.02 · token aceito em TTL_acesso − 1 ms")
    void rn06_ct06_02_aceitoAntesDoTtl() {
        ParDeTokens par = sessao.login(EMAIL, SENHA, "celular");
        c.relogio.avancar(TTL_ACESSO.minus(UM_MS));

        assertEquals(usuario, sessao.validar(par.tokenDeAcesso()).usuarioId());
    }

    @Test
    @Disabled("Bloqueado: AM-01, no instante exato do TTL o token expira?")
    @DisplayName("RN-06 · CT-06.03 · token exatamente em TTL_acesso")
    void rn06_ct06_03_bordaDoTtl() {
    }

    @Test
    @DisplayName("RN-06 · CT-06.04 · token em TTL_acesso + 1 ms → token_expirado")
    void rn06_ct06_04_expiradoAposTtl() {
        ParDeTokens par = sessao.login(EMAIL, SENHA, "celular");
        c.relogio.avancar(TTL_ACESSO.plus(UM_MS));

        assertEquals(CodigoDeErro.TOKEN_EXPIRADO, codigoDaFalha(() -> sessao.validar(par.tokenDeAcesso())));
    }

    @Test
    @DisplayName("RN-06 · CT-06.05 · token malformado → token_invalido")
    void rn06_ct06_05_tokenMalformado() {
        assertEquals(CodigoDeErro.TOKEN_INVALIDO, codigoDaFalha(() -> sessao.validar("@@ malformado @@")));
    }

    @Test
    @DisplayName("RN-06 · CT-06.06 · token sem sessão correspondente → token_invalido")
    void rn06_ct06_06_tokenInexistente() {
        sessao.login(EMAIL, SENHA, "celular");

        assertEquals(CodigoDeErro.TOKEN_INVALIDO,
                codigoDaFalha(() -> sessao.validar(UUID.randomUUID().toString())));
    }

    @Test
    @DisplayName("RN-06 · CT-06.07 · token de sessão revogada → token_invalido, nunca aceito")
    void rn06_ct06_07_tokenDeSessaoRevogada() {
        ParDeTokens par = sessao.login(EMAIL, SENHA, "celular");
        sessao.revogarTodas(usuario);

        assertEquals(CodigoDeErro.TOKEN_INVALIDO, codigoDaFalha(() -> sessao.validar(par.tokenDeAcesso())));
    }

    @Test
    @Disabled("Bloqueado: QT-04, expirado e revogado ao mesmo tempo: qual código prevalece?")
    @DisplayName("RN-06 · CT-06.08 · token expirado e revogado")
    void rn06_ct06_08_expiradoERevogado() {
    }

    // ---------------- RN-07 Renovação (docs/arquitetura.md:619) ----------------

    @Test
    @DisplayName("RN-07 · CT-07.01 · renovação emite novo par e o novo refresh passa a ser o atual")
    void rn07_ct07_01_renovacaoRotaciona() {
        ParDeTokens p0 = sessao.login(EMAIL, SENHA, "celular");

        ParDeTokens p1 = sessao.renovar(p0.refreshToken(), "celular");

        assertAll(
                () -> assertNotEquals(p0, p1),
                () -> assertEquals(usuario, sessao.validar(p1.tokenDeAcesso()).usuarioId()),
                () -> assertDoesNotThrow(() -> sessao.renovar(p1.refreshToken(), "celular")));
    }

    @Test
    @DisplayName("RN-07 · CT-07.02 · refresh anterior, mesmo dispositivo, em tolerância − 1 ms → mesmo par")
    void rn07_ct07_02_reusoDentroDaTolerancia() {
        ParDeTokens p0 = sessao.login(EMAIL, SENHA, "celular");
        ParDeTokens p1 = sessao.renovar(p0.refreshToken(), "celular");
        c.relogio.avancar(TOLERANCIA_REUSO.minus(UM_MS));

        ParDeTokens repetido = sessao.renovar(p0.refreshToken(), "celular");

        assertEquals(p1, repetido);
        assertEquals(usuario, sessao.validar(p1.tokenDeAcesso()).usuarioId());
    }

    @Test
    @DisplayName("RN-07 · CT-07.03 · refresh anterior em tolerância + 1 ms → sessão revogada")
    void rn07_ct07_03_reusoForaDaToleranciaRevoga() {
        ParDeTokens p0 = sessao.login(EMAIL, SENHA, "celular");
        ParDeTokens p1 = sessao.renovar(p0.refreshToken(), "celular");
        c.relogio.avancar(TOLERANCIA_REUSO.plus(UM_MS));

        assertEquals(CodigoDeErro.TOKEN_INVALIDO, codigoDaFalha(() -> sessao.renovar(p0.refreshToken(), "celular")));
        assertEquals(CodigoDeErro.TOKEN_INVALIDO, codigoDaFalha(() -> sessao.validar(p1.tokenDeAcesso())));
    }

    @Test
    @DisplayName("RN-07 · CT-07.04 · refresh anterior de outro dispositivo, dentro da tolerância → revoga (AM-10)")
    void rn07_ct07_04_reusoDeOutroDispositivoRevoga() {
        ParDeTokens p0 = sessao.login(EMAIL, SENHA, "celular");
        ParDeTokens p1 = sessao.renovar(p0.refreshToken(), "celular");

        assertEquals(CodigoDeErro.TOKEN_INVALIDO, codigoDaFalha(() -> sessao.renovar(p0.refreshToken(), "outro")));
        assertEquals(CodigoDeErro.TOKEN_INVALIDO, codigoDaFalha(() -> sessao.validar(p1.tokenDeAcesso())));
    }

    @Test
    @DisplayName("RN-07 · CT-07.05 · refresh mais antigo que o anterior → sessão revogada")
    void rn07_ct07_05_refreshMaisAntigoRevoga() {
        ParDeTokens p0 = sessao.login(EMAIL, SENHA, "celular");
        ParDeTokens p1 = sessao.renovar(p0.refreshToken(), "celular");
        ParDeTokens p2 = sessao.renovar(p1.refreshToken(), "celular");

        assertEquals(CodigoDeErro.TOKEN_INVALIDO, codigoDaFalha(() -> sessao.renovar(p0.refreshToken(), "celular")));
        assertEquals(CodigoDeErro.TOKEN_INVALIDO, codigoDaFalha(() -> sessao.validar(p2.tokenDeAcesso())));
    }

    @Test
    @DisplayName("RN-07 · CT-07.06a · renovação aceita em TTL_refresh − 1 ms")
    void rn07_ct07_06a_renovacaoAntesDoTtlRefresh() {
        ParDeTokens p0 = sessao.login(EMAIL, SENHA, "celular");
        c.relogio.avancar(TTL_REFRESH.minus(UM_MS));

        assertDoesNotThrow(() -> sessao.renovar(p0.refreshToken(), "celular"));
    }

    @Test
    @DisplayName("RN-07 · CT-07.06b · renovação rejeitada em TTL_refresh + 1 ms")
    void rn07_ct07_06b_renovacaoAposTtlRefresh() {
        ParDeTokens p0 = sessao.login(EMAIL, SENHA, "celular");
        c.relogio.avancar(TTL_REFRESH.plus(UM_MS));

        assertThrows(FalhaDeAcesso.class, () -> sessao.renovar(p0.refreshToken(), "celular"));
    }

    @Test
    @DisplayName("RN-07 · CT-07.07 · refresh adulterado ou de sessão revogada → token_invalido, nenhum par")
    void rn07_ct07_07_refreshInvalido() {
        ParDeTokens p0 = sessao.login(EMAIL, SENHA, "celular");

        assertEquals(CodigoDeErro.TOKEN_INVALIDO, codigoDaFalha(() -> sessao.renovar("adulterado", "celular")));
        sessao.logout(p0.tokenDeAcesso());
        assertEquals(CodigoDeErro.TOKEN_INVALIDO, codigoDaFalha(() -> sessao.renovar(p0.refreshToken(), "celular")));
    }

    @Test
    @DisplayName("RN-07 · CT-07.08 · após renovar, o par antigo deixa de valer (nunca dois pares válidos)")
    void rn07_ct07_08_nuncaDoisParesValidos() {
        ParDeTokens p0 = sessao.login(EMAIL, SENHA, "celular");

        ParDeTokens p1 = sessao.renovar(p0.refreshToken(), "celular");

        assertEquals(CodigoDeErro.TOKEN_INVALIDO, codigoDaFalha(() -> sessao.validar(p0.tokenDeAcesso())));
        assertEquals(usuario, sessao.validar(p1.tokenDeAcesso()).usuarioId());
    }

    // ---------------- RN-08 Renovação concorrente (docs/arquitetura.md:620) ----------------

    private List<ParDeTokens> renovarAoMesmoTempo(List<Callable<ParDeTokens>> tarefas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tarefas.size());
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<ParDeTokens>> futuros = tarefas.stream().map(t -> pool.submit(() -> {
                largada.await();
                return t.call();
            })).toList();
            largada.countDown();
            return futuros.stream().map(f -> {
                try {
                    return f.get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new AssertionError("renovação concorrente falhou: " + e.getCause(), e);
                }
            }).toList();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("RN-08 · CT-08.01 · duas renovações simultâneas na mesma sessão recebem o mesmo par")
    void rn08_ct08_01_corridaNaMesmaSessao() throws Exception {
        ParDeTokens p0 = sessao.login(EMAIL, SENHA, "celular");

        List<ParDeTokens> resultados = renovarAoMesmoTempo(List.of(
                () -> sessao.renovar(p0.refreshToken(), "celular"),
                () -> sessao.renovar(p0.refreshToken(), "celular")));

        assertEquals(resultados.get(0), resultados.get(1));
        assertEquals(usuario, sessao.validar(resultados.get(0).tokenDeAcesso()).usuarioId());
    }

    @Test
    @DisplayName("RN-08 · CT-08.02 · renovações simultâneas em dispositivos diferentes vencem as duas")
    void rn08_ct08_02_corridaEmDispositivosDiferentes() throws Exception {
        ParDeTokens celular = sessao.login(EMAIL, SENHA, "celular");
        ParDeTokens computador = sessao.login(EMAIL, SENHA, "computador");

        List<ParDeTokens> r = renovarAoMesmoTempo(List.of(
                () -> sessao.renovar(celular.refreshToken(), "celular"),
                () -> sessao.renovar(computador.refreshToken(), "computador")));

        assertNotEquals(r.get(0), r.get(1));
        assertEquals(usuario, sessao.validar(r.get(0).tokenDeAcesso()).usuarioId());
        assertEquals(usuario, sessao.validar(r.get(1).tokenDeAcesso()).usuarioId());
    }

    @Test
    @DisplayName("RN-08 · CT-08.03 · após a corrida existe um único par válido para a sessão")
    void rn08_ct08_03_umUnicoParAposCorrida() throws Exception {
        ParDeTokens p0 = sessao.login(EMAIL, SENHA, "celular");

        List<ParDeTokens> r = renovarAoMesmoTempo(List.of(
                () -> sessao.renovar(p0.refreshToken(), "celular"),
                () -> sessao.renovar(p0.refreshToken(), "celular"),
                () -> sessao.renovar(p0.refreshToken(), "celular")));

        Set<ParDeTokens> distintos = new HashSet<>(r);
        assertEquals(1, distintos.size());
    }

    // ---------------- RN-09 Logout (docs/arquitetura.md:621) ----------------

    @Test
    @DisplayName("RN-09 · CT-09.01 · após logout o token de acesso é recusado imediatamente")
    void rn09_ct09_01_logoutInvalidaTokenNaHora() {
        ParDeTokens par = sessao.login(EMAIL, SENHA, "celular");

        sessao.logout(par.tokenDeAcesso());

        assertEquals(CodigoDeErro.TOKEN_INVALIDO, codigoDaFalha(() -> sessao.validar(par.tokenDeAcesso())));
    }

    @Test
    @DisplayName("RN-09 · CT-09.02 · após logout o refresh da sessão é recusado")
    void rn09_ct09_02_logoutInvalidaRefresh() {
        ParDeTokens par = sessao.login(EMAIL, SENHA, "celular");

        sessao.logout(par.tokenDeAcesso());

        assertThrows(FalhaDeAcesso.class, () -> sessao.renovar(par.refreshToken(), "celular"));
    }

    @Test
    @DisplayName("RN-09 · CT-09.03 · logout no dispositivo A mantém a sessão do dispositivo B (Q-05 pode mudar)")
    void rn09_ct09_03_logoutSoDoDispositivoAtual() {
        ParDeTokens a = sessao.login(EMAIL, SENHA, "celular");
        ParDeTokens b = sessao.login(EMAIL, SENHA, "computador");

        sessao.logout(a.tokenDeAcesso());

        assertEquals(usuario, sessao.validar(b.tokenDeAcesso()).usuarioId());
    }

    @Test
    @Disabled("Bloqueado: QT-05, resposta do logout com token já inválido não definida")
    @DisplayName("RN-09 · CT-09.04 · logout com token inválido")
    void rn09_ct09_04_logoutComTokenInvalido() {
    }

    @Test
    @DisplayName("RN-09 · CT-09.05 · sessão revogada nunca volta a aceitar tokens")
    void rn09_ct09_05_sessaoRevogadaNaoRessuscita() {
        ParDeTokens par = sessao.login(EMAIL, SENHA, "celular");
        sessao.logout(par.tokenDeAcesso());

        assertThrows(FalhaDeAcesso.class, () -> sessao.renovar(par.refreshToken(), "celular"));
        c.relogio.avancar(TOLERANCIA_REUSO.multipliedBy(2));

        assertEquals(CodigoDeErro.TOKEN_INVALIDO, codigoDaFalha(() -> sessao.validar(par.tokenDeAcesso())));
    }
}
