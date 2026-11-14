package br.senac.footfanatics.esportes.http;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuração do adapter HTTP, vinda do ambiente (propriedades {@code sports.provider.*}, que leem
 * SPORTS_PROVIDER_URL e SPORTS_PROVIDER_TOKEN). Nenhum valor real fica no código.
 * {@link #toString()} nunca mostra o token.
 */
public record ConfiguracaoSportsProvider(URI baseUrl, String token, Duration connectTimeout, Duration readTimeout,
                                         int maxResponseBytes) {

    public ConfiguracaoSportsProvider {
        Objects.requireNonNull(baseUrl, "baseUrl");
        Objects.requireNonNull(connectTimeout, "connectTimeout");
        Objects.requireNonNull(readTimeout, "readTimeout");
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token do provedor ausente (SPORTS_PROVIDER_TOKEN)");
        }
        if (maxResponseBytes <= 0) {
            throw new IllegalArgumentException("maxResponseBytes deve ser positivo");
        }
    }

    @Override
    public String toString() {
        return "ConfiguracaoSportsProvider[baseUrl=" + baseUrl + ", token=***, connectTimeout=" + connectTimeout
                + ", readTimeout=" + readTimeout + ", maxResponseBytes=" + maxResponseBytes + "]";
    }
}
