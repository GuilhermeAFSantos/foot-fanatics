package br.senac.footfanatics.treino;

public class ErroDeDados extends RuntimeException {
    public ErroDeDados(String mensagem) {
        super(mensagem);
    }
}
