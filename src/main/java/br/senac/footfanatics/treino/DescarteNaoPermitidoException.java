package br.senac.footfanatics.treino;

public class DescarteNaoPermitidoException extends RuntimeException {
    public DescarteNaoPermitidoException(String mensagem) {
        super(mensagem);
    }
}
