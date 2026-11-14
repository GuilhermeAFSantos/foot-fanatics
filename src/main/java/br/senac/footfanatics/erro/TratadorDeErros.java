package br.senac.footfanatics.erro;

import br.senac.footfanatics.esportes.FalhaNaIntegracaoException;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Converte falhas em contrato de erro estável no formato Problem Details (ADR-014).
 * Status e nomes de código vêm de docs/arquitetura.md §4.1/§4.2 e do laboratório da Aula 08 (INT-01 a INT-05).
 * Nunca expõe a causa interna nem conteúdo de terceiros. O nome da propriedade "codigo" é provisório (Q-07).
 */
public class TratadorDeErros {

    public ProblemDetail paraProblema(FalhaDeAcesso falha) {
        return problema(falha.codigo());
    }

    /**
     * Falha do provedor de esportes: código e status próprios, mais a correlação para suporte.
     * Nada do provedor (corpo, header, status original) atravessa.
     */
    public ProblemDetail paraProblema(FalhaNaIntegracaoException falha) {
        CodigoDeErro codigo = switch (falha.motivo()) {
            case NAO_ENCONTRADO -> CodigoDeErro.JOGO_NAO_ENCONTRADO;
            case INDISPONIVEL, RECUSADA -> CodigoDeErro.INTEGRACAO_INDISPONIVEL;
            case RESPOSTA_INVALIDA -> CodigoDeErro.RESPOSTA_EXTERNA_INVALIDA;
        };
        ProblemDetail problema = problema(codigo);
        problema.setProperty("correlacao", falha.correlacao().toString());
        return problema;
    }

    private static ProblemDetail problema(CodigoDeErro codigo) {
        HttpStatus status = statusDe(codigo);
        ProblemDetail problema = ProblemDetail.forStatus(status);
        problema.setTitle(status.getReasonPhrase());
        problema.setProperty("codigo", codigo.name().toLowerCase(Locale.ROOT));
        return problema;
    }

    private static HttpStatus statusDe(CodigoDeErro codigo) {
        return switch (codigo) {
            case AUTENTICACAO_NECESSARIA, TOKEN_AUSENTE, TOKEN_EXPIRADO, TOKEN_INVALIDO, TOKEN_REVOGADO ->
                    HttpStatus.UNAUTHORIZED;
            case PLANO_INSUFICIENTE, ACESSO_NEGADO -> HttpStatus.FORBIDDEN;
            case JOGO_NAO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case INTEGRACAO_INDISPONIVEL -> HttpStatus.SERVICE_UNAVAILABLE;
            // Provisório: o material da aula não fixa o status; 502 indica resposta inválida de um servidor upstream.
            case RESPOSTA_EXTERNA_INVALIDA -> HttpStatus.BAD_GATEWAY;
        };
    }
}
