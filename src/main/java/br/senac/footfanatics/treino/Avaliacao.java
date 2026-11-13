package br.senac.footfanatics.treino;

import java.util.OptionalDouble;

public record Avaliacao(boolean sanidadeAprovada, double metricaCandidato, OptionalDouble metricaAtivo) {
}
