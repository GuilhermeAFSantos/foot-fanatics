package br.senac.footfanatics.treino;

import java.util.Optional;
import java.util.UUID;

/** Métricas pendentes (Q-09). */
public interface AvaliadorDeCandidato {
    Avaliacao avaliar(CandidatoTreinado candidato, Optional<UUID> versaoAtiva);
}
