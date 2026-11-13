package br.senac.footfanatics.treino;

import java.util.UUID;

public record VersaoDoModelo(UUID id, StatusVersao status, UUID execucaoId, UUID baseVersaoId,
                             String hashArtefato, String versaoCodigo, String parametros) {
}
