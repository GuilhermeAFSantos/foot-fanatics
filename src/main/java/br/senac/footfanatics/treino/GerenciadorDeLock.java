package br.senac.footfanatics.treino;

public interface GerenciadorDeLock {
    boolean tentarAdquirir();

    void liberar();
}
