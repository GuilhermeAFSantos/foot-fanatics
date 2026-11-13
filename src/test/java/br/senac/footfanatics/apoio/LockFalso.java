package br.senac.footfanatics.apoio;

import br.senac.footfanatics.treino.GerenciadorDeLock;

public class LockFalso implements GerenciadorDeLock {
    public boolean disponivel = true;
    public int liberacoes;

    @Override
    public boolean tentarAdquirir() {
        return disponivel;
    }

    @Override
    public void liberar() {
        liberacoes++;
    }
}
