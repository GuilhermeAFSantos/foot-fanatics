package br.senac.footfanatics.assinatura;

import java.time.Instant;
import java.util.UUID;

public record Assinatura(UUID id, UUID usuarioId, Plano plano,
                         Instant inicioVigencia, Instant fimVigencia, Instant canceladaEm) {
}
