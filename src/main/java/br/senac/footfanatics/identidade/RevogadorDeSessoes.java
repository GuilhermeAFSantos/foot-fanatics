package br.senac.footfanatics.identidade;

import java.util.UUID;

public interface RevogadorDeSessoes {
    void revogarTodas(UUID usuarioId);
}
