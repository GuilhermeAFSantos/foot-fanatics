package br.senac.footfanatics.treino;

public class ErroTransitorio extends RuntimeException {
    public ErroTransitorio(String mensagem) {
        super(mensagem);
    }
}
