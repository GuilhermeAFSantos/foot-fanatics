package br.senac.footfanatics.treino;

import java.time.Duration;

/** Parâmetros do Job: todos com valor pendente (Q-15, ADR-011, §6.3 itens 6, 8 e 12, Q-09). */
public record ConfiguracaoDoJob(Duration margem,
                                int maxReprovacoesConsecutivas,
                                int maxTentativas,
                                double limiteDeQuarentena,
                                int versaoDeEsquemaAtual,
                                double toleranciaDoGate,
                                double piso) {
}
