package br.senac.footfanatics.treino;

public record ResultadoDoJob(Desfecho desfecho, boolean alertaEmitido) {

    public enum Desfecho {
        IGNORADA_LOCK_INDISPONIVEL,
        SEM_EFEITO_JA_CONCLUIDA,
        CONCLUIDA,
        FALHOU,
        REPROVADA,
        PARADA_AGUARDANDO_DECISAO
    }
}
