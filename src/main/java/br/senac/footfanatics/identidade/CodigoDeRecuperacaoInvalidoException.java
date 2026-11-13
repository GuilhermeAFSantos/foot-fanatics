package br.senac.footfanatics.identidade;

public class CodigoDeRecuperacaoInvalidoException extends RuntimeException {
    public CodigoDeRecuperacaoInvalidoException() {
        super("código de recuperação inválido");
    }
}
