package br.senac.footfanatics.erro;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ProblemDetail;

/**
 * RN-21: contrato de erro estável (ADR-014, Proposta). Status e nomes de código vêm de §4.1/§4.2.
 * O nome da propriedade que carrega o código não está definido (Q-07), por isso o teste
 * procura o valor entre as propriedades, sem fixar a chave.
 */
class ContratoDeErroTest {

    private final TratadorDeErros tratador = new TratadorDeErros();

    static Stream<Arguments> contrato() {
        return Stream.of(
                Arguments.of(CodigoDeErro.AUTENTICACAO_NECESSARIA, 401),
                Arguments.of(CodigoDeErro.TOKEN_EXPIRADO, 401),
                Arguments.of(CodigoDeErro.TOKEN_INVALIDO, 401),
                Arguments.of(CodigoDeErro.PLANO_INSUFICIENTE, 402), // QUEBRA PROPOSITAL para validar o CI (será revertida)
                Arguments.of(CodigoDeErro.ACESSO_NEGADO, 403));
    }

    private static String nomeDoContrato(CodigoDeErro codigo) {
        return codigo.name().toLowerCase(Locale.ROOT);
    }

    @ParameterizedTest(name = "RN-21 · CT-21.01 · {0} → HTTP {1} com código estável")
    @MethodSource("contrato")
    void rn21_ct21_01_problemDetailsComCodigoEstavel(CodigoDeErro codigo, int status) {
        ProblemDetail problema = tratador.paraProblema(new FalhaDeAcesso(codigo));

        assertAll(
                () -> assertEquals(status, problema.getStatus()),
                () -> assertTrue(problema.getProperties() != null
                        && problema.getProperties().containsValue(nomeDoContrato(codigo))));
    }

    @Test
    @DisplayName("RN-21 · CT-21.02 · a mesma situação sempre produz o mesmo status e código")
    void rn21_ct21_02_mesmaSituacaoMesmoCodigo() {
        ProblemDetail a = tratador.paraProblema(new FalhaDeAcesso(CodigoDeErro.PLANO_INSUFICIENTE));
        ProblemDetail b = tratador.paraProblema(
                new FalhaDeAcesso(CodigoDeErro.PLANO_INSUFICIENTE, new IllegalStateException("outro contexto")));

        assertEquals(a.getStatus(), b.getStatus());
        assertEquals(a.getProperties(), b.getProperties());
    }

    @Test
    @DisplayName("RN-21 · CT-21.03 · corpo de erro nunca expõe detalhe interno")
    void rn21_ct21_03_semDetalheInterno() {
        FalhaDeAcesso falha = new FalhaDeAcesso(CodigoDeErro.ACESSO_NEGADO,
                new SQLException("SELECT hash_senha FROM app.usuario WHERE id = 42"));

        ProblemDetail problema = tratador.paraProblema(falha);

        String corpo = String.valueOf(problema.getDetail()) + problema.getTitle() + problema.getProperties();
        assertAll(
                () -> assertFalse(corpo.contains("SELECT")),
                () -> assertFalse(corpo.contains("SQLException")),
                () -> assertFalse(corpo.contains("hash_senha")),
                () -> assertFalse(corpo.contains("at br.senac")));
    }
}
