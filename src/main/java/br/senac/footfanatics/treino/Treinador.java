package br.senac.footfanatics.treino;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Treino incremental sobre a versão base (ADR-012). Algoritmo pendente (Q-09). */
public interface Treinador {
    CandidatoTreinado treinar(Optional<UUID> baseVersaoId, List<EventoDeEntrada> dadosNovos);
}
