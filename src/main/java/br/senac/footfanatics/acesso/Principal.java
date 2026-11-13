package br.senac.footfanatics.acesso;

import java.util.UUID;

/** Quem faz a requisição. usuarioId nulo = anônimo. */
public record Principal(UUID usuarioId) {

    public static Principal anonimo() {
        return new Principal(null);
    }

    public static Principal de(UUID usuarioId) {
        return new Principal(usuarioId);
    }
}
