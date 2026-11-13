package br.senac.footfanatics.sessao;

public class CredenciaisInvalidasException extends RuntimeException {
    public CredenciaisInvalidasException() {
        super("credenciais inválidas");
    }
}
