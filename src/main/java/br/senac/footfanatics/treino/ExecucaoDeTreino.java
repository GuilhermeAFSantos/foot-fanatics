package br.senac.footfanatics.treino;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExecucaoDeTreino(UUID id, LocalDate dataReferencia, StatusExecucao status,
                               Instant watermarkInicio, Instant corte, String hashDados) {
}
