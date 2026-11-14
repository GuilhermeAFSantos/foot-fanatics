package br.senac.footfanatics.esportes;

/**
 * Status do jogo no modelo interno. O mapeamento a partir do provedor está no adapter;
 * valor desconhecido do provedor é rejeitado, nunca adivinhado.
 */
public enum StatusJogo {
    AGENDADO,
    EM_ANDAMENTO,
    ENCERRADO
}
