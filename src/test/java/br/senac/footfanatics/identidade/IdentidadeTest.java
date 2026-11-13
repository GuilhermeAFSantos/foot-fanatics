package br.senac.footfanatics.identidade;

import static br.senac.footfanatics.apoio.ValoresDeTeste.T0;
import static br.senac.footfanatics.apoio.ValoresDeTeste.TOLERANCIA_REUSO;
import static br.senac.footfanatics.apoio.ValoresDeTeste.TTL_ACESSO;
import static br.senac.footfanatics.apoio.ValoresDeTeste.TTL_REFRESH;
import static br.senac.footfanatics.apoio.ValoresDeTeste.UM_MS;
import static br.senac.footfanatics.apoio.ValoresDeTeste.VALIDADE_CODIGO;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.senac.footfanatics.apoio.CanalDeRecuperacaoFalso;
import br.senac.footfanatics.apoio.CodigosEmMemoria;
import br.senac.footfanatics.apoio.RelogioAjustavel;
import br.senac.footfanatics.apoio.SessoesEmMemoria;
import br.senac.footfanatics.apoio.UsuariosEmMemoria;
import br.senac.footfanatics.erro.CodigoDeErro;
import br.senac.footfanatics.erro.FalhaDeAcesso;
import br.senac.footfanatics.sessao.ConfiguracaoDeSessao;
import br.senac.footfanatics.sessao.ParDeTokens;
import br.senac.footfanatics.sessao.ServicoDeSessao;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** E1 Identidade & Conta: RN-01 a RN-04 (docs/testes/plano-tdd.md §2). */
class IdentidadeTest {

    private static final String EMAIL = "bia@exemplo.test";
    private static final String SENHA = "senha-original-123";

    private RelogioAjustavel relogio;
    private UsuariosEmMemoria usuarios;
    private CodigosEmMemoria codigos;
    private CanalDeRecuperacaoFalso canal;
    private ServicoDeIdentidade identidade;
    private ServicoDeSessao sessao;

    @BeforeEach
    void montar() {
        relogio = new RelogioAjustavel(T0);
        usuarios = new UsuariosEmMemoria();
        codigos = new CodigosEmMemoria();
        canal = new CanalDeRecuperacaoFalso();
        AtomicReference<ServicoDeSessao> ref = new AtomicReference<>();
        identidade = new ServicoDeIdentidade(usuarios, codigos, canal, id -> ref.get().revogarTodas(id),
                relogio, new ConfiguracaoDeRecuperacao(VALIDADE_CODIGO));
        sessao = new ServicoDeSessao(identidade, new SessoesEmMemoria(), relogio,
                new ConfiguracaoDeSessao(TTL_ACESSO, TTL_REFRESH, TOLERANCIA_REUSO));
        ref.set(sessao);
    }

    // ---------------- RN-01 Cadastro (README.md:9) ----------------

    @Test
    @DisplayName("RN-01 · CT-01.01 · cadastro com e-mail e senha permite autenticar")
    void rn01_ct01_01_cadastroPermiteAutenticar() {
        // Arrange: só e-mail e senha (os demais campos são LG-01)
        // Act
        UUID id = identidade.cadastrar(EMAIL, SENHA);
        // Assert
        assertEquals(Optional.of(id), identidade.autenticar(EMAIL, SENHA));
    }

    @Test
    @Disabled("Bloqueado: LG-01, campos obrigatórios e código de erro do cadastro não definidos")
    @DisplayName("RN-01 · CT-01.02 · cadastro com campo obrigatório ausente")
    void rn01_ct01_02_campoObrigatorioAusente() {
    }

    @Test
    @Disabled("Bloqueado: QT-01, unicidade do e-mail não escrita em nenhum artefato")
    @DisplayName("RN-01 · CT-01.03 · cadastro com e-mail já usado")
    void rn01_ct01_03_emailDuplicado() {
    }

    @Test
    @Disabled("Bloqueado: LG-01, regra de conta sem credencial só implícita")
    @DisplayName("RN-01 · CT-01.04 · conta sem credencial não pode existir")
    void rn01_ct01_04_contaSemCredencial() {
    }

    // ---------------- RN-02 Credencial só como hash (§1.1; ADR-015) ----------------

    @Test
    @DisplayName("RN-02 · CT-02.01 · credencial armazenada difere da senha e continua verificável")
    void rn02_ct02_01_credencialArmazenadaComoHash() {
        // Arrange / Act
        UUID id = identidade.cadastrar(EMAIL, SENHA);
        // Assert
        Usuario salvo = usuarios.buscarPorId(id).orElseThrow();
        assertAll(
                () -> assertNotEquals(SENHA, salvo.hashSenha()),
                () -> assertFalse(salvo.hashSenha().contains(SENHA)),
                () -> assertEquals(Optional.of(id), identidade.autenticar(EMAIL, SENHA)));
    }

    @Test
    @DisplayName("RN-02 · CT-02.02 · senha correta autentica")
    void rn02_ct02_02_senhaCorretaAutentica() {
        UUID id = identidade.cadastrar(EMAIL, SENHA);

        Optional<UUID> autenticado = identidade.autenticar(EMAIL, SENHA);

        assertEquals(Optional.of(id), autenticado);
    }

    @Test
    @DisplayName("RN-02 · CT-02.03 · senha errada não autentica")
    void rn02_ct02_03_senhaErradaNaoAutentica() {
        identidade.cadastrar(EMAIL, SENHA);

        Optional<UUID> autenticado = identidade.autenticar(EMAIL, "senha-errada");

        assertTrue(autenticado.isEmpty());
    }

    @Test
    @DisplayName("RN-02 · CT-02.04 · senha nunca aparece em log (ADR-015)")
    void rn02_ct02_04_senhaNuncaEmLog() {
        // Arrange
        Logger raiz = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> captura = new ListAppender<>();
        captura.start();
        raiz.addAppender(captura);
        try {
            // Act
            identidade.cadastrar(EMAIL, SENHA);
            identidade.autenticar(EMAIL, SENHA);
            identidade.autenticar(EMAIL, "outra-senha-qualquer");
            sessao.login(EMAIL, SENHA, "celular");
        } finally {
            raiz.detachAppender(captura);
        }
        // Assert
        assertTrue(captura.list.stream().noneMatch(e -> e.getFormattedMessage().contains(SENHA)));
    }

    @Test
    @Disabled("Bloqueado: L-06, sem política de credencial")
    @DisplayName("RN-02 · CT-02.05 · senha fora da política")
    void rn02_ct02_05_senhaForaDaPolitica() {
    }

    // ---------------- RN-03 Perfil (README.md:9) ----------------

    @Test
    @Disabled("Sem regra: LG-02, o artefato só diz que o perfil existe (docs/arquitetura.md D6.1)")
    @DisplayName("RN-03 · (sem caso) · perfil de usuário")
    void rn03_semCasoPossivel() {
    }

    // ---------------- RN-04 Recuperação de acesso (docs/arquitetura.md:623) ----------------

    @Test
    @DisplayName("RN-04 · CT-04.01 · código válido recupera o acesso e revoga todas as sessões")
    void rn04_ct04_01_recuperaERevogaSessoes() {
        // Arrange
        identidade.cadastrar(EMAIL, SENHA);
        ParDeTokens antiga = sessao.login(EMAIL, SENHA, "computador");
        identidade.solicitarRecuperacao(EMAIL);
        String codigo = canal.ultimoCodigoPara(EMAIL);
        // Act
        identidade.redefinirSenha(codigo, "senha-nova-456");
        // Assert
        FalhaDeAcesso falha = assertThrows(FalhaDeAcesso.class, () -> sessao.validar(antiga.tokenDeAcesso()));
        assertAll(
                () -> assertEquals(CodigoDeErro.TOKEN_INVALIDO, falha.codigo()),
                () -> assertTrue(identidade.autenticar(EMAIL, "senha-nova-456").isPresent()),
                () -> assertTrue(identidade.autenticar(EMAIL, SENHA).isEmpty()));
    }

    @Test
    @DisplayName("RN-04 · CT-04.02 · resposta idêntica para e-mail existente e inexistente")
    void rn04_ct04_02_respostaIdenticaParaEmailInexistente() {
        identidade.cadastrar(EMAIL, SENHA);

        RespostaDeRecuperacao existente = identidade.solicitarRecuperacao(EMAIL);
        RespostaDeRecuperacao inexistente = identidade.solicitarRecuperacao("ninguem@exemplo.test");

        assertEquals(existente, inexistente);
    }

    @Test
    @DisplayName("RN-04 · CT-04.03 · código de recuperação é de uso único")
    void rn04_ct04_03_codigoDeUsoUnico() {
        identidade.cadastrar(EMAIL, SENHA);
        identidade.solicitarRecuperacao(EMAIL);
        String codigo = canal.ultimoCodigoPara(EMAIL);
        identidade.redefinirSenha(codigo, "senha-nova-456");

        assertThrows(CodigoDeRecuperacaoInvalidoException.class,
                () -> identidade.redefinirSenha(codigo, "senha-terceira-789"));
    }

    @Test
    @DisplayName("RN-04 · CT-04.04a · código aceito em validade − 1 ms")
    void rn04_ct04_04a_codigoAceitoAntesDaValidade() {
        identidade.cadastrar(EMAIL, SENHA);
        identidade.solicitarRecuperacao(EMAIL);
        String codigo = canal.ultimoCodigoPara(EMAIL);
        relogio.avancar(VALIDADE_CODIGO.minus(UM_MS));

        assertDoesNotThrow(() -> identidade.redefinirSenha(codigo, "senha-nova-456"));
    }

    @Test
    @DisplayName("RN-04 · CT-04.04b · código rejeitado em validade + 1 ms")
    void rn04_ct04_04b_codigoRejeitadoAposValidade() {
        identidade.cadastrar(EMAIL, SENHA);
        identidade.solicitarRecuperacao(EMAIL);
        String codigo = canal.ultimoCodigoPara(EMAIL);
        relogio.avancar(VALIDADE_CODIGO.plus(UM_MS));

        assertThrows(CodigoDeRecuperacaoInvalidoException.class,
                () -> identidade.redefinirSenha(codigo, "senha-nova-456"));
    }

    @Test
    @Disabled("Bloqueado: AM-01, no instante exato da validade o código vale ou não?")
    @DisplayName("RN-04 · CT-04.04c · código exatamente na validade")
    void rn04_ct04_04c_codigoNaBordaDaValidade() {
    }

    @Test
    @DisplayName("RN-04 · CT-04.05 · código adulterado é rejeitado sem revogar sessões")
    void rn04_ct04_05_codigoAdulteradoNaoRevoga() {
        identidade.cadastrar(EMAIL, SENHA);
        ParDeTokens par = sessao.login(EMAIL, SENHA, "celular");
        identidade.solicitarRecuperacao(EMAIL);

        assertThrows(CodigoDeRecuperacaoInvalidoException.class,
                () -> identidade.redefinirSenha("codigo-adulterado", "senha-nova-456"));
        assertDoesNotThrow(() -> sessao.validar(par.tokenDeAcesso()));
    }

    @Test
    @DisplayName("RN-04 · CT-04.06 · código nunca é guardado em texto puro")
    void rn04_ct04_06_codigoGuardadoSoComoHash() {
        identidade.cadastrar(EMAIL, SENHA);

        identidade.solicitarRecuperacao(EMAIL);

        String codigo = canal.ultimoCodigoPara(EMAIL);
        assertFalse(codigos.listarTodos().isEmpty());
        assertTrue(codigos.listarTodos().stream().noneMatch(c -> c.toString().contains(codigo)));
    }

    @Test
    @Disabled("Bloqueado: QT-02, novo código invalida o anterior?")
    @DisplayName("RN-04 · CT-04.07 · dois códigos válidos em sequência")
    void rn04_ct04_07_doisCodigosEmSequencia() {
    }
}
