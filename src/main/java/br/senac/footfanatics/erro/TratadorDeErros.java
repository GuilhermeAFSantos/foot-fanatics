package br.senac.footfanatics.erro;

import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Converte falhas em contrato de erro estável no formato Problem Details (ADR-014).
 * Status e nomes de código vêm de docs/arquitetura.md §4.1/§4.2. Nunca expõe a causa interna.
 * O nome da propriedade "codigo" é provisório (Q-07).
 */
public class TratadorDeErros {

    public ProblemDetail paraProblema(FalhaDeAcesso falha) {
        HttpStatus status = statusDe(falha.codigo());
        ProblemDetail problema = ProblemDetail.forStatus(status);
        problema.setTitle(status.getReasonPhrase());
        problema.setProperty("codigo", falha.codigo().name().toLowerCase(Locale.ROOT));
        return problema;
    }

    private static HttpStatus statusDe(CodigoDeErro codigo) {
        return switch (codigo) {
            case AUTENTICACAO_NECESSARIA, TOKEN_EXPIRADO, TOKEN_INVALIDO -> HttpStatus.UNAUTHORIZED;
            case PLANO_INSUFICIENTE, ACESSO_NEGADO -> HttpStatus.FORBIDDEN;
        };
    }
}
