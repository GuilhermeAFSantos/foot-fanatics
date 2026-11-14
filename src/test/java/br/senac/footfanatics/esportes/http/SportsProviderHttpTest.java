package br.senac.footfanatics.esportes.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import br.senac.footfanatics.apoio.JogosEmMemoria;
import br.senac.footfanatics.erro.TratadorDeErros;
import br.senac.footfanatics.esportes.FalhaNaIntegracaoException;
import br.senac.footfanatics.esportes.FalhaNaIntegracaoException.Motivo;
import br.senac.footfanatics.esportes.JogoExterno;
import br.senac.footfanatics.esportes.ServicoDeJogos;
import br.senac.footfanatics.esportes.StatusJogo;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;

/**
 * Laboratório da Aula 08: contrato da porta HTTP do provedor de esportes, com WireMock em porta aleatória.
 * Tudo roda offline (só 127.0.0.1) e é determinístico: a resposta está sob controle do teste.
 * O token é fictício ("token-de-teste"); nenhum valor real aparece aqui.
 */
class SportsProviderHttpTest {

    private static final String TOKEN = "token-de-teste";
    /**
     * Os mesmos limites de application.properties. Um limite justo demais tornaria o teste instável: o primeiro
     * request da JVM (WireMock e HttpClient "frios") já levou ~1 s localmente.
     */
    private static final Duration CONNECT_TIMEOUT = Duration.ofMillis(500);
    private static final Duration READ_TIMEOUT = Duration.ofMillis(1500);
    /** Bem acima do read-timeout, para o INT-04 nunca ficar na fronteira. */
    private static final int ATRASO_MAIOR_QUE_O_LIMITE_MS = 5000;
    private static final int LIMITE_DE_BYTES = 1024;

    @RegisterExtension
    static WireMockExtension provedor = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private JogosEmMemoria jogos;
    private SportsProviderHttp client;
    private ServicoDeJogos servico;
    private ListAppender<ILoggingEvent> logs;
    private Logger logger;

    @BeforeEach
    void montar() {
        ConfiguracaoSportsProvider configuracao = new ConfiguracaoSportsProvider(
                URI.create(provedor.baseUrl()), TOKEN, CONNECT_TIMEOUT, READ_TIMEOUT, LIMITE_DE_BYTES);
        client = new SportsProviderHttp(configuracao);
        jogos = new JogosEmMemoria();
        servico = new ServicoDeJogos(client, jogos);
        logger = (Logger) LoggerFactory.getLogger(SportsProviderHttp.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void soltarLogs() {
        logger.detachAppender(logs);
    }

    private String todoOLog() {
        return logs.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining("\n"));
    }

    private FalhaNaIntegracaoException falhaAoSincronizar(long id) {
        return catchThrowableOfType(FalhaNaIntegracaoException.class, () -> servico.sincronizar(id));
    }

    // ---------------- INT-01: 200 válido → modelo interno ----------------

    @Test
    @DisplayName("INT-01 · 200 válido com partida encerrada → mapeia o modelo interno e respeita o contrato do request")
    void int01_partidaConcluidaMapeadaComRequestNoContrato() {
        provedor.stubFor(get(urlEqualTo("/matches/42"))
                .withHeader("X-Api-Key", equalTo(TOKEN))
                .willReturn(okJson("""
                        {"id":42,"status":"FINISHED","home":2,"away":1}
                        """)));

        JogoExterno jogo = servico.sincronizar(42);

        assertThat(jogo).isEqualTo(new JogoExterno(42, StatusJogo.ENCERRADO, 2, 1));
        assertThat(jogos.buscarPorIdExterno(42)).contains(jogo);
        provedor.verify(1, getRequestedFor(urlEqualTo("/matches/42"))
                .withHeader("X-Api-Key", equalTo(TOKEN))
                .withHeader("X-Correlation-Id", matching("[0-9a-f-]{36}"))
                .withHeader("Accept", equalTo("application/json")));
    }

    @ParameterizedTest(name = "status do provedor {0} → {1}")
    @MethodSource("statusConhecidos")
    @DisplayName("INT-01 · cada status declarado do provedor vira um status interno")
    void int01_statusDoProvedorMapeado(String doProvedor, StatusJogo interno) {
        provedor.stubFor(get(urlEqualTo("/matches/7")).willReturn(okJson(
                "{\"id\":7,\"status\":\"" + doProvedor + "\",\"home\":0,\"away\":0}")));

        assertThat(client.buscarJogo(7).status()).isEqualTo(interno);
    }

    static List<Arguments> statusConhecidos() {
        return List.of(
                Arguments.of("SCHEDULED", StatusJogo.AGENDADO),
                Arguments.of("IN_PLAY", StatusJogo.EM_ANDAMENTO),
                Arguments.of("FINISHED", StatusJogo.ENCERRADO));
    }

    // ---------------- INT-02: 404 → 404 próprio ----------------

    @Test
    @DisplayName("INT-02 · 404 do provedor → falha NAO_ENCONTRADO, 404 jogo_nao_encontrado próprio e nada persistido")
    void int02_jogoInexistenteVira404Proprio() {
        provedor.stubFor(get(urlEqualTo("/matches/999")).willReturn(aResponse().withStatus(404)
                .withHeader("Content-Type", "application/json").withBody("{\"message\":\"match not found\"}")));

        FalhaNaIntegracaoException falha = falhaAoSincronizar(999);
        ProblemDetail problema = new TratadorDeErros().paraProblema(falha);

        assertThat(falha.motivo()).isEqualTo(Motivo.NAO_ENCONTRADO);
        assertThat(problema.getStatus()).isEqualTo(404);
        assertThat(problema.getProperties()).containsEntry("codigo", "jogo_nao_encontrado");
        assertThat(problema.toString()).doesNotContain("match not found");
        assertThat(jogos.quantidade()).isZero();
    }

    // ---------------- INT-03: 500 → 503 controlado ----------------

    @ParameterizedTest(name = "provedor responde {0} → 503 integracao_indisponivel, uma única chamada")
    @ValueSource(ints = {500, 502, 503})
    @DisplayName("INT-03 · 5xx do provedor → 503 controlado, sem nova tentativa e sem persistir")
    void int03_erroDoProvedorVira503Controlado(int status) {
        provedor.stubFor(get(urlEqualTo("/matches/42")).willReturn(aResponse().withStatus(status)));

        FalhaNaIntegracaoException falha = falhaAoSincronizar(42);
        ProblemDetail problema = new TratadorDeErros().paraProblema(falha);

        assertThat(falha.motivo()).isEqualTo(Motivo.INDISPONIVEL);
        assertThat(problema.getStatus()).isEqualTo(503);
        assertThat(problema.getProperties()).containsEntry("codigo", "integracao_indisponivel");
        assertThat(jogos.quantidade()).isZero();
        provedor.verify(1, getRequestedFor(urlEqualTo("/matches/42")));
    }

    @Test
    @DisplayName("INT-03 · provedor fora do ar (conexão recusada) → 503 controlado e nada persistido")
    void int03_provedorForaDoArVira503() throws IOException {
        int portaFechada;
        try (ServerSocket reservada = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            portaFechada = reservada.getLocalPort();
        } // fechada aqui: ninguém escuta nessa porta local
        ServicoDeJogos semProvedor = new ServicoDeJogos(new SportsProviderHttp(new ConfiguracaoSportsProvider(
                URI.create("http://127.0.0.1:" + portaFechada + "/"), TOKEN, CONNECT_TIMEOUT, READ_TIMEOUT,
                LIMITE_DE_BYTES)), jogos);

        FalhaNaIntegracaoException falha =
                catchThrowableOfType(FalhaNaIntegracaoException.class, () -> semProvedor.sincronizar(42));

        assertThat(falha.motivo()).isEqualTo(Motivo.INDISPONIVEL);
        assertThat(new TratadorDeErros().paraProblema(falha).getStatus()).isEqualTo(503);
        assertThat(jogos.quantidade()).isZero();
    }

    // ---------------- INT-04: resposta lenta → interrompe e não persiste ----------------

    @Test
    @DisplayName("INT-04 · resposta mais lenta que o read-timeout → interrompe com INDISPONIVEL e não persiste")
    void int04_timeoutInterrompeENaoPersiste() {
        provedor.stubFor(get(urlEqualTo("/matches/42")).willReturn(okJson(
                "{\"id\":42,\"status\":\"FINISHED\",\"home\":2,\"away\":1}")
                .withFixedDelay(ATRASO_MAIOR_QUE_O_LIMITE_MS)));

        long inicio = System.nanoTime();
        FalhaNaIntegracaoException falha = falhaAoSincronizar(42);
        Duration decorrido = Duration.ofNanos(System.nanoTime() - inicio);

        assertThat(falha.motivo()).isEqualTo(Motivo.INDISPONIVEL);
        assertThat(jogos.buscarPorIdExterno(42)).isEmpty();
        // Tolerância larga: só prova que o cliente desistiu antes de a resposta (atrasada 5 s) chegar.
        assertThat(decorrido).isLessThan(Duration.ofMillis(ATRASO_MAIOR_QUE_O_LIMITE_MS));
    }

    // ---------------- INT-05: JSON inválido → rejeita e registra correlação ----------------

    @ParameterizedTest(name = "{0}")
    @MethodSource("corposInvalidos")
    @DisplayName("INT-05 · 200 fora do contrato → RESPOSTA_INVALIDA, nada persistido e correlação no log")
    void int05_corpoInvalidoRejeitadoComCorrelacao(String caso, String contentType, String corpo) {
        provedor.stubFor(get(urlEqualTo("/matches/42")).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", contentType).withBody(corpo)));

        FalhaNaIntegracaoException falha = falhaAoSincronizar(42);
        ProblemDetail problema = new TratadorDeErros().paraProblema(falha);

        assertThat(falha.motivo()).isEqualTo(Motivo.RESPOSTA_INVALIDA);
        assertThat(jogos.quantidade()).isZero();
        assertThat(problema.getStatus()).isEqualTo(502);
        assertThat(problema.getProperties())
                .containsEntry("codigo", "resposta_externa_invalida")
                .containsEntry("correlacao", falha.correlacao().toString());
        assertThat(todoOLog()).contains("correlacao=" + falha.correlacao()).contains("motivo=RESPOSTA_INVALIDA");
        provedor.verify(getRequestedFor(urlEqualTo("/matches/42"))
                .withHeader("X-Correlation-Id", equalTo(falha.correlacao().toString())));
    }

    static List<Arguments> corposInvalidos() {
        String json = "application/json";
        return List.of(
                Arguments.of("JSON malformado", json, "{\"id\":42,\"status\":"),
                Arguments.of("campo obrigatório ausente (away)", json, "{\"id\":42,\"status\":\"FINISHED\",\"home\":2}"),
                Arguments.of("status desconhecido", json, "{\"id\":42,\"status\":\"POSTPONED_FOREVER\",\"home\":2,\"away\":1}"),
                Arguments.of("tipo incompatível (home texto)", json, "{\"id\":42,\"status\":\"FINISHED\",\"home\":\"dois\",\"away\":1}"),
                Arguments.of("placar fracionário", json, "{\"id\":42,\"status\":\"FINISHED\",\"home\":2.5,\"away\":1}"),
                Arguments.of("placar negativo", json, "{\"id\":42,\"status\":\"FINISHED\",\"home\":-1,\"away\":1}"),
                Arguments.of("id diferente do pedido", json, "{\"id\":43,\"status\":\"FINISHED\",\"home\":2,\"away\":1}"),
                Arguments.of("corpo null", json, "null"),
                Arguments.of("content type não JSON", "text/html", "<html>manutenção</html>"),
                Arguments.of("corpo acima do limite", json,
                        "{\"id\":42,\"status\":\"FINISHED\",\"home\":2,\"away\":1,\"extra\":\"" + "x".repeat(LIMITE_DE_BYTES) + "\"}"));
    }

    // ---------------- Derivados do material (§5 e §6 da aula) ----------------

    @ParameterizedTest(name = "provedor responde {0} → RECUSADA, uma única chamada")
    @ValueSource(ints = {400, 401, 403, 429})
    @DisplayName("INT-06 · 4xx diferente de 404 é permanente: 503 controlado, sem repetir a chamada")
    void int06_quatrocentosNaoSaoRepetidos(int status) {
        provedor.stubFor(get(urlEqualTo("/matches/42")).willReturn(aResponse().withStatus(status)));

        FalhaNaIntegracaoException falha = falhaAoSincronizar(42);

        assertThat(falha.motivo()).isEqualTo(Motivo.RECUSADA);
        assertThat(new TratadorDeErros().paraProblema(falha).getStatus()).isEqualTo(503);
        assertThat(jogos.quantidade()).isZero();
        provedor.verify(1, getRequestedFor(urlEqualTo("/matches/42")));
    }

    @Test
    @DisplayName("INT-07 · token devolvido no corpo de erro do provedor não atravessa a API, a exceção nem o log")
    void int07_tokenDoProvedorNaoVaza() {
        String corpoComSegredo = "{\"error\":\"invalid key " + TOKEN + "\",\"echo\":{\"X-Api-Key\":\"" + TOKEN
                + "\"},\"trace\":\"at com.provider.Auth(Auth.java:12)\"}";
        provedor.stubFor(get(urlEqualTo("/matches/42")).willReturn(aResponse().withStatus(500)
                .withHeader("Content-Type", "application/json").withBody(corpoComSegredo)));

        FalhaNaIntegracaoException falha = falhaAoSincronizar(42);
        ProblemDetail problema = new TratadorDeErros().paraProblema(falha);

        String respostaDaApi = problema.toString() + problema.getProperties() + problema.getDetail() + problema.getTitle();
        assertThat(respostaDaApi).doesNotContain(TOKEN).doesNotContain("invalid key").doesNotContain("Auth.java");
        assertThat(falha.getMessage()).doesNotContain(TOKEN);
        assertThat(falha.getCause()).isNull();
        assertThat(todoOLog()).doesNotContain(TOKEN).doesNotContain("invalid key");
    }

    @Test
    @DisplayName("INT-07 · o log de uma chamada bem-sucedida tem correlação, status e duração, mas nunca o token")
    void int07_logSemCredencial() {
        provedor.stubFor(get(urlEqualTo("/matches/42")).willReturn(okJson(
                "{\"id\":42,\"status\":\"FINISHED\",\"home\":2,\"away\":1}")));

        client.buscarJogo(42);

        assertThat(todoOLog()).contains("correlacao=").contains("status=200").contains("duracaoMs=")
                .doesNotContain(TOKEN);
    }

    @Test
    @DisplayName("Configuração exige token e nunca o imprime")
    void configuracaoNuncaMostraOToken() {
        ConfiguracaoSportsProvider configuracao = new ConfiguracaoSportsProvider(
                URI.create("http://127.0.0.1:1"), TOKEN, Duration.ofMillis(1), Duration.ofMillis(1), 1);

        assertThat(configuracao.toString()).doesNotContain(TOKEN).contains("token=***");
        assertThatThrownBy(() -> new ConfiguracaoSportsProvider(
                URI.create("http://127.0.0.1:1"), " ", Duration.ofMillis(1), Duration.ofMillis(1), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
