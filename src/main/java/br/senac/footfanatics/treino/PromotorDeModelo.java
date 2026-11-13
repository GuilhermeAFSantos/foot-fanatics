package br.senac.footfanatics.treino;

import java.util.UUID;

/** Único componente que altera o ponteiro do modelo ativo (INV-3; ADR-010). */
public class PromotorDeModelo {

    public PromotorDeModelo(RegistryDeModelos registry) {
    }

    public void rollback(String responsavel) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    public void apontarPara(UUID versaoId, String responsavel) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }
}
