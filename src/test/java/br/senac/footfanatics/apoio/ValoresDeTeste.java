package br.senac.footfanatics.apoio;

import java.time.Duration;
import java.time.Instant;

/**
 * Valores ARBITRÁRIOS usados só para parametrizar os testes. NÃO são requisitos:
 * os valores reais estão pendentes (Q-04, EXP-10, L-05, Q-15, ADR-011, §6.3 itens 6, 8 e 12, Q-09).
 * Os testes verificam a regra RELATIVA ao parâmetro configurado, não o valor.
 */
public final class ValoresDeTeste {
    public static final Instant T0 = Instant.parse("2026-11-13T12:00:00Z");
    public static final Duration TTL_ACESSO = Duration.ofMinutes(10);
    public static final Duration TTL_REFRESH = Duration.ofDays(7);
    public static final Duration TOLERANCIA_REUSO = Duration.ofSeconds(5);
    public static final Duration VALIDADE_CODIGO = Duration.ofMinutes(15);
    public static final Duration MARGEM = Duration.ofMinutes(5);
    public static final int MAX_REPROVACOES = 2;
    public static final int MAX_TENTATIVAS = 3;
    public static final double LIMITE_QUARENTENA = 0.10;
    public static final int VERSAO_ESQUEMA_ATUAL = 3;
    /** 0.25 e 0.5 são exatos em binário: evita falso vermelho/verde por arredondamento no caso-limite. */
    public static final double TOLERANCIA_GATE = 0.25;
    public static final double PISO = 0.50;
    public static final Duration UM_MS = Duration.ofMillis(1);

    private ValoresDeTeste() {
    }
}
