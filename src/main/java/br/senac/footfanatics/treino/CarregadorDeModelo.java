package br.senac.footfanatics.treino;

import java.util.UUID;

public interface CarregadorDeModelo {
    /** Lança ArtefatoInvalidoException quando o artefato não carrega. */
    ModeloCarregado carregar(UUID versaoId, byte[] artefato);
}
