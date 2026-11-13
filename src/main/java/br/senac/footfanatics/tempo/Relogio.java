package br.senac.footfanatics.tempo;

import java.time.Instant;

/** Fonte única de "agora" (ADR-005). */
public interface Relogio {
    Instant agora();
}
