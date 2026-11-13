package br.senac.footfanatics.erro;

import org.springframework.http.ProblemDetail;

/** Converte falhas em contrato de erro estável (ADR-014). */
public class TratadorDeErros {

    public ProblemDetail paraProblema(FalhaDeAcesso falha) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }
}
