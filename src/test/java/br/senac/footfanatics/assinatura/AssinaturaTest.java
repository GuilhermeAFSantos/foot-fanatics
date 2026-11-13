package br.senac.footfanatics.assinatura;

import static br.senac.footfanatics.apoio.ValoresDeTeste.T0;
import static br.senac.footfanatics.apoio.ValoresDeTeste.UM_MS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.senac.footfanatics.acesso.Classificacao;
import br.senac.footfanatics.acesso.Principal;
import br.senac.footfanatics.apoio.CenarioDeAcesso;
import br.senac.footfanatics.erro.CodigoDeErro;
import br.senac.footfanatics.erro.FalhaDeAcesso;
import br.senac.footfanatics.sessao.ParDeTokens;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** E3 Assinatura: RN-10 a RN-14 (README.md:11; ADR-003, ADR-005). */
class AssinaturaTest {

    private static final Instant INICIO = T0;
    private static final Instant FIM = T0.plus(Duration.ofDays(30));

    private CenarioDeAcesso c;

    @BeforeEach
    void montar() {
        c = new CenarioDeAcesso();
    }

    private void exclusivoPermitido(UUID usuario) {
        assertDoesNotThrow(() -> c.politica.exigirAcessoAoConteudo(Principal.de(usuario), Classificacao.EXCLUSIVA));
    }

    private void exclusivoNegadoPorPlano(UUID usuario) {
        FalhaDeAcesso f = assertThrows(FalhaDeAcesso.class,
                () -> c.politica.exigirAcessoAoConteudo(Principal.de(usuario), Classificacao.EXCLUSIVA));
        assertEquals(CodigoDeErro.PLANO_INSUFICIENTE, f.codigo());
    }

    // ---------------- RN-10 Planos free e premium (README.md:11) ----------------

    @Test
    @Disabled("Bloqueado: QT-06, todo cadastro começa no free?")
    @DisplayName("RN-10 · CT-10.01 · atribuição de plano no cadastro")
    void rn10_ct10_01_planoInicial() {
    }

    @Test
    @DisplayName("RN-10 · CT-10.02 · só existem os planos free e premium")
    void rn10_ct10_02_soFreeEPremium() {
        // Act / Assert
        assertArrayEquals(new Plano[] {Plano.FREE, Plano.PREMIUM}, Plano.values());
        assertThrows(IllegalArgumentException.class, () -> Plano.valueOf("GOLD"));
    }

    @Test
    @Disabled("Bloqueado: QT-07, mais de uma assinatura vigente é permitida?")
    @DisplayName("RN-10 · CT-10.03 · dois planos vigentes ao mesmo tempo")
    void rn10_ct10_03_doisPlanosVigentes() {
    }

    // ---------------- RN-11 Vigência (docs/arquitetura.md:606; ADR-003, ADR-005) ----------------

    @Test
    @DisplayName("RN-11 · CT-11.01 · premium dentro da vigência acessa conteúdo exclusivo")
    void rn11_ct11_01_dentroDaVigencia() {
        UUID u = c.usuarioPremium(INICIO, FIM);
        c.relogio.definir(INICIO.plus(Duration.ofDays(1)));

        exclusivoPermitido(u);
    }

    @Test
    @DisplayName("RN-11 · CT-11.02 · início da vigência − 1 ms → plano_insuficiente")
    void rn11_ct11_02_antesDoInicio() {
        UUID u = c.usuarioPremium(INICIO, FIM);
        c.relogio.definir(INICIO.minus(UM_MS));

        exclusivoNegadoPorPlano(u);
    }

    @Test
    @Disabled("Bloqueado: AM-01, no instante exato do início a vigência vale?")
    @DisplayName("RN-11 · CT-11.03 · exatamente no início da vigência")
    void rn11_ct11_03_bordaDoInicio() {
    }

    @Test
    @DisplayName("RN-11 · CT-11.04 · fim da vigência − 1 ms → permitido")
    void rn11_ct11_04_antesDoFim() {
        UUID u = c.usuarioPremium(INICIO, FIM);
        c.relogio.definir(FIM.minus(UM_MS));

        exclusivoPermitido(u);
    }

    @Test
    @Disabled("Bloqueado: AM-01, no instante exato do fim a vigência vale?")
    @DisplayName("RN-11 · CT-11.05 · exatamente no fim da vigência")
    void rn11_ct11_05_bordaDoFim() {
    }

    @Test
    @DisplayName("RN-11 · CT-11.06 · fim da vigência + 1 ms → plano_insuficiente")
    void rn11_ct11_06_aposOFim() {
        UUID u = c.usuarioPremium(INICIO, FIM);
        c.relogio.definir(FIM.plus(UM_MS));

        exclusivoNegadoPorPlano(u);
    }

    @Test
    @Disabled("Bloqueado: QT-08, validação de fim < início não escrita")
    @DisplayName("RN-11 · CT-11.07 · assinatura com fim antes do início")
    void rn11_ct11_07_fimAntesDoInicio() {
    }

    // ---------------- RN-12 Upgrade (README.md:11) ----------------

    @Test
    @Disabled("Bloqueado: Q-02 / S-04, mecanismo do upgrade sem pagamento real desconhecido")
    @DisplayName("RN-12 · CT-12.01 · free faz upgrade e acessa exclusivo")
    void rn12_ct12_01_upgradeLiberaExclusivo() {
    }

    @Test
    @Disabled("Bloqueado: QT-09, premium vigente pode pedir upgrade?")
    @DisplayName("RN-12 · CT-12.02 · upgrade pedido por premium vigente")
    void rn12_ct12_02_upgradeDePremium() {
    }

    @Test
    @Disabled("Bloqueado: QT-07, upgrade pode gerar duas assinaturas vigentes?")
    @DisplayName("RN-12 · CT-12.03 · upgrade gerando duas assinaturas vigentes")
    void rn12_ct12_03_upgradeDuplicado() {
    }

    // ---------------- RN-13 Cancelamento (docs/arquitetura.md:622; ADR-003) ----------------

    @Test
    @Disabled("Bloqueado: Q-03, cancelamento corta na hora ou no fim da vigência (t_corte)?")
    @DisplayName("RN-13 · CT-13.01 · exclusivo negado após t_corte com o mesmo token")
    void rn13_ct13_01_exclusivoNegadoAposCorte() {
    }

    @Test
    @Disabled("Bloqueado: Q-03 (t_corte) e AM-01 (borda)")
    @DisplayName("RN-13 · CT-13.02 · exclusivo em t_corte ± 1 ms")
    void rn13_ct13_02_bordaDoCorte() {
    }

    @Test
    @DisplayName("RN-13 · CT-13.03 · após cancelar, conteúdo público continua permitido")
    void rn13_ct13_03_publicoAposCancelar() {
        UUID u = c.usuarioPremiumVigente();

        c.assinatura.cancelar(u);

        assertDoesNotThrow(() -> c.politica.exigirAcessoAoConteudo(Principal.de(u), Classificacao.PUBLICA));
    }

    @Test
    @DisplayName("RN-13 · CT-13.04 · após cancelar, dados da própria conta permitidos e sessão não revogada")
    void rn13_ct13_04_contaESessaoAposCancelar() {
        String email = "premium@exemplo.test";
        UUID u = c.autenticador.conhecer(email, "s");
        c.assinaturas.salvar(new Assinatura(UUID.randomUUID(), u, Plano.PREMIUM,
                T0.minus(Duration.ofDays(1)), T0.plus(Duration.ofDays(30)), null));
        ParDeTokens par = c.sessao.login(email, "s", "celular");

        c.assinatura.cancelar(u);

        assertDoesNotThrow(() -> c.politica.exigirAcessoAosDadosDe(Principal.de(u), u));
        assertEquals(u, c.sessao.validar(par.tokenDeAcesso()).usuarioId());
    }

    @Test
    @Disabled("Bloqueado: QT-10, cancelar o plano free")
    @DisplayName("RN-13 · CT-13.05 · free tenta cancelar")
    void rn13_ct13_05_cancelarFree() {
    }

    @Test
    @Disabled("Bloqueado: QT-10, cancelar duas vezes")
    @DisplayName("RN-13 · CT-13.06 · cancelar duas vezes")
    void rn13_ct13_06_cancelarDuasVezes() {
    }

    // ---------------- RN-14 Plano fora do token (ADR-003) ----------------

    @Test
    @DisplayName("RN-14 · CT-14.01 · token emitido durante a vigência não mantém o acesso depois do fim")
    void rn14_ct14_01_tokenNaoCarregaPrivilegio() {
        // Arrange: vigência termina antes do token expirar (fim = T0 + 5 min < TTL de teste)
        String email = "premium@exemplo.test";
        UUID u = c.autenticador.conhecer(email, "s");
        c.assinaturas.salvar(new Assinatura(UUID.randomUUID(), u, Plano.PREMIUM,
                T0.minus(Duration.ofDays(1)), T0.plus(Duration.ofMinutes(5)), null));
        ParDeTokens par = c.sessao.login(email, "s", "celular");
        c.relogio.definir(T0.plus(Duration.ofMinutes(5)).plus(UM_MS));

        // Act
        Principal principal = c.sessao.validar(par.tokenDeAcesso());

        // Assert
        FalhaDeAcesso f = assertThrows(FalhaDeAcesso.class,
                () -> c.politica.exigirAcessoAoConteudo(principal, Classificacao.EXCLUSIVA));
        assertEquals(CodigoDeErro.PLANO_INSUFICIENTE, f.codigo());
    }

    @Test
    @Disabled("Bloqueado: Q-02, mecanismo do upgrade")
    @DisplayName("RN-14 · CT-14.02 · upgrade vale sem novo login")
    void rn14_ct14_02_upgradeSemNovoLogin() {
    }
}
