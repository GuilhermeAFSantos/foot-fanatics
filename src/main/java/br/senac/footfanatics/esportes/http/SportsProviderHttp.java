package br.senac.footfanatics.esportes.http;

import br.senac.footfanatics.esportes.FalhaNaIntegracaoException;
import br.senac.footfanatics.esportes.FalhaNaIntegracaoException.Motivo;
import br.senac.footfanatics.esportes.JogoExterno;
import br.senac.footfanatics.esportes.SportsProvider;
import br.senac.footfanatics.esportes.StatusJogo;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adapter HTTP do {@link SportsProvider} (Aula 08). Contrato declarado do provedor:
 * {@code GET {base}/matches/{id}} com header {@code X-Api-Key}; resposta 200 {@code application/json}
 * {@code {"id": inteiro, "status": "SCHEDULED"|"IN_PLAY"|"FINISHED", "home": inteiro ≥ 0, "away": inteiro ≥ 0}}.
 *
 * <ul>
 *   <li>404 → {@link Motivo#NAO_ENCONTRADO}; outro 4xx → {@link Motivo#RECUSADA} (permanente, sem nova tentativa);
 *       5xx, timeout ou rede → {@link Motivo#INDISPONIVEL}. Nenhuma nova tentativa automática nesta versão.</li>
 *   <li>200 só vale com content type JSON, tamanho dentro do limite, campos obrigatórios, tipos corretos, status
 *       conhecido, id igual ao pedido e placar não negativo; senão {@link Motivo#RESPOSTA_INVALIDA}.</li>
 *   <li>Log: só correlação, id pedido, status HTTP, duração e motivo. Nunca token, headers ou corpo.</li>
 * </ul>
 */
public class SportsProviderHttp implements SportsProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SportsProviderHttp.class);
    static final String HEADER_CHAVE = "X-Api-Key";
    static final String HEADER_CORRELACAO = "X-Correlation-Id";

    private static final Map<String, StatusJogo> STATUS = Map.of(
            "SCHEDULED", StatusJogo.AGENDADO,
            "IN_PLAY", StatusJogo.EM_ANDAMENTO,
            "FINISHED", StatusJogo.ENCERRADO);

    /** Formato declarado da resposta. Tipos em caixa para distinguir "ausente" de zero. */
    record PartidaDoProvedor(Long id, String status, Integer home, Integer away) {
    }

    private final ConfiguracaoSportsProvider configuracao;
    private final HttpClient http;
    private final JsonMapper json = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // campo novo do provedor não quebra
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)        // 2.5 gols não vira 2
            .build();

    public SportsProviderHttp(ConfiguracaoSportsProvider configuracao) {
        this.configuracao = configuracao;
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(configuracao.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public JogoExterno buscarJogo(long idExterno) {
        UUID correlacao = UUID.randomUUID();
        long inicio = System.nanoTime();
        HttpRequest request = HttpRequest.newBuilder(uri("matches/" + idExterno))
                .timeout(configuracao.readTimeout())
                .header(HEADER_CHAVE, configuracao.token())
                .header(HEADER_CORRELACAO, correlacao.toString())
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<InputStream> resposta;
        try {
            resposta = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException erroDeRede) {
            throw falha(Motivo.INDISPONIVEL, correlacao, idExterno, -1, inicio);
        } catch (InterruptedException interrompida) {
            Thread.currentThread().interrupt();
            throw falha(Motivo.INDISPONIVEL, correlacao, idExterno, -1, inicio);
        }

        try (InputStream corpo = resposta.body()) {
            int status = resposta.statusCode();
            if (status == 404) {
                throw falha(Motivo.NAO_ENCONTRADO, correlacao, idExterno, status, inicio);
            }
            if (status >= 400 && status < 500) {
                throw falha(Motivo.RECUSADA, correlacao, idExterno, status, inicio);
            }
            if (status != 200) {
                throw falha(Motivo.INDISPONIVEL, correlacao, idExterno, status, inicio);
            }
            JogoExterno jogo = mapear(idExterno, resposta, corpo);
            if (jogo == null) {
                throw falha(Motivo.RESPOSTA_INVALIDA, correlacao, idExterno, status, inicio);
            }
            LOG.info("sports-provider correlacao={} jogo={} status={} duracaoMs={}",
                    correlacao, idExterno, status, duracaoMs(inicio));
            return jogo;
        } catch (IOException leituraInterrompida) {
            throw falha(Motivo.INDISPONIVEL, correlacao, idExterno, resposta.statusCode(), inicio);
        }
    }

    /** Devolve o jogo mapeado ou null se a resposta não respeitar o contrato. Nunca guarda a exceção do parser. */
    private JogoExterno mapear(long idPedido, HttpResponse<InputStream> resposta, InputStream corpo)
            throws IOException {
        String tipo = resposta.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
        if (!tipo.startsWith("application/json")) {
            return null;
        }
        byte[] bytes = corpo.readNBytes(configuracao.maxResponseBytes() + 1);
        if (bytes.length > configuracao.maxResponseBytes()) {
            return null;
        }
        PartidaDoProvedor partida;
        try {
            partida = json.readValue(bytes, PartidaDoProvedor.class);
        } catch (JacksonException conteudoInvalido) {
            return null;
        }
        if (partida == null || partida.id() == null || partida.status() == null
                || partida.home() == null || partida.away() == null) {
            return null;
        }
        StatusJogo status = STATUS.get(partida.status());
        if (status == null || partida.id() != idPedido || partida.home() < 0 || partida.away() < 0) {
            return null;
        }
        return new JogoExterno(partida.id(), status, partida.home(), partida.away());
    }

    private FalhaNaIntegracaoException falha(Motivo motivo, UUID correlacao, long idExterno, int status, long inicio) {
        LOG.warn("sports-provider correlacao={} jogo={} status={} duracaoMs={} motivo={}",
                correlacao, idExterno, status, duracaoMs(inicio), motivo);
        return new FalhaNaIntegracaoException(motivo, correlacao);
    }

    private URI uri(String caminho) {
        String base = configuracao.baseUrl().toString();
        return URI.create(base.endsWith("/") ? base + caminho : base + "/" + caminho);
    }

    private static long duracaoMs(long inicio) {
        return Duration.ofNanos(System.nanoTime() - inicio).toMillis();
    }
}
