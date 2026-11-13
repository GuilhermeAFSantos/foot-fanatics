package br.senac.footfanatics.identidade;

import java.time.Instant;
import java.util.UUID;

public record CodigoDeRecuperacao(UUID id, UUID usuarioId, String hashCodigo, Instant emitidoEm, Instant usadoEm) {
}
