package br.senac.footfanatics.sessao;

import java.time.Duration;

/** TTLs e tolerância: valores pendentes (Q-04, EXP-10). */
public record ConfiguracaoDeSessao(Duration ttlAcesso, Duration ttlRefresh, Duration toleranciaDeReuso) {
}
