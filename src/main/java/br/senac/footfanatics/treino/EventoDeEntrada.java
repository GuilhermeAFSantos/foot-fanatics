package br.senac.footfanatics.treino;

import java.time.Instant;
import java.util.UUID;

/** EVENTO_DE_ENTRADA (docs/arquitetura.md §3). */
public record EventoDeEntrada(long seq, UUID idEvento, String tipo, int versaoEsquema,
                              String sujeitoPseudonimo, Instant ocorridoEm, Instant registradoEm) {
}
