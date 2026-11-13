package br.senac.footfanatics.sessao;

import java.time.Instant;
import java.util.UUID;

/** SESSAO por dispositivo (docs/arquitetura.md §3; ADR-004). */
public record Sessao(UUID id, UUID usuarioId, String dispositivo,
                     String hashTokenDeAcesso, Instant acessoExpiraEm,
                     String hashRefreshToken, Instant refreshExpiraEm,
                     String hashRefreshAnterior, Instant renovadaEm,
                     int versao, Instant revogadaEm) {
}
