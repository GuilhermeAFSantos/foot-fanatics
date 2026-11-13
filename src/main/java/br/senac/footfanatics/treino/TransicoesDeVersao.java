package br.senac.footfanatics.treino;

/** Transições permitidas de §6.4; qualquer outra é proibida. */
public final class TransicoesDeVersao {

    private TransicoesDeVersao() {
    }

    public static void exigirPermitida(StatusVersao de, StatusVersao para) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }
}
