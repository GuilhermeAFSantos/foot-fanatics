package br.senac.footfanatics.erro;

/** Falha de autenticação ou autorização com código estável. */
public class FalhaDeAcesso extends RuntimeException {

    private final CodigoDeErro codigo;

    public FalhaDeAcesso(CodigoDeErro codigo) {
        this(codigo, null);
    }

    public FalhaDeAcesso(CodigoDeErro codigo, Throwable causa) {
        super(String.valueOf(codigo), causa);
        this.codigo = codigo;
    }

    public CodigoDeErro codigo() {
        return codigo;
    }
}
