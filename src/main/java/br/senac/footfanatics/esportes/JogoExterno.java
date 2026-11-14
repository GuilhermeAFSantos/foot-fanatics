package br.senac.footfanatics.esportes;

import java.util.Objects;

/** Jogo no modelo interno, já validado. Placar nunca negativo. */
public record JogoExterno(long idExterno, StatusJogo status, int golsMandante, int golsVisitante) {

    public JogoExterno {
        Objects.requireNonNull(status, "status");
        if (golsMandante < 0 || golsVisitante < 0) {
            throw new IllegalArgumentException("placar negativo");
        }
    }
}
