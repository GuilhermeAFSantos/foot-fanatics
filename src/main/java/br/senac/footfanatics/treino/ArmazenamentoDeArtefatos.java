package br.senac.footfanatics.treino;

import java.util.Optional;

/** Tecnologia adiada (ADR-013). */
public interface ArmazenamentoDeArtefatos {
    String gravar(byte[] artefato);

    Optional<byte[]> ler(String hashArtefato);
}
