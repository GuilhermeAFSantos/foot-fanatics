package br.senac.footfanatics.acesso;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.senac.footfanatics.apoio.CenarioDeAcesso;
import br.senac.footfanatics.erro.CodigoDeErro;
import br.senac.footfanatics.erro.FalhaDeAcesso;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RN-17 e RN-18: dados de conta, sessão e assinatura (docs/arquitetura.md:607-608; ADR-002). */
class DadosDeUsuarioTest {

    /** Códigos da família 401 em §4.1/§4.2. A matriz diz "401" sem código para dados de conta (AM-03). */
    private static final Set<CodigoDeErro> FAMILIA_401 =
            Set.of(CodigoDeErro.AUTENTICACAO_NECESSARIA, CodigoDeErro.TOKEN_INVALIDO, CodigoDeErro.TOKEN_EXPIRADO);

    private CenarioDeAcesso c;

    @BeforeEach
    void montar() {
        c = new CenarioDeAcesso();
    }

    private CodigoDeErro negado(Principal p, UUID dono) {
        return assertThrows(FalhaDeAcesso.class, () -> c.politica.exigirAcessoAosDadosDe(p, dono)).codigo();
    }

    // ---------------- RN-17 Próprios dados ----------------

    @Test
    @DisplayName("RN-17 · CT-17.01 · free e premium acessam os próprios dados")
    void rn17_ct17_01_propriosDadosPermitidos() {
        UUID free = c.usuarioFree();
        UUID premium = c.usuarioPremiumVigente();

        assertDoesNotThrow(() -> c.politica.exigirAcessoAosDadosDe(Principal.de(free), free));
        assertDoesNotThrow(() -> c.politica.exigirAcessoAosDadosDe(Principal.de(premium), premium));
    }

    @Test
    @DisplayName("RN-17 · CT-17.02 · anônimo em dados de conta → 401")
    void rn17_ct17_02_anonimoEmDadosDeConta() {
        UUID dono = c.usuarioFree();

        assertTrue(FAMILIA_401.contains(negado(Principal.anonimo(), dono)));
    }

    @Test
    @DisplayName("RN-17 · CT-17.03 · premium vencido acessa os próprios dados")
    void rn17_ct17_03_premiumVencidoPropriosDados() {
        UUID u = c.usuarioPremiumVencido();

        assertDoesNotThrow(() -> c.politica.exigirAcessoAosDadosDe(Principal.de(u), u));
    }

    // ---------------- RN-18 Dados de outro usuário ----------------

    @Test
    @DisplayName("RN-18 · CT-18.01 · free em dados de outro usuário → acesso_negado")
    void rn18_ct18_01_freeEmOutroUsuario() {
        UUID outro = c.usuarioFree();

        assertEquals(CodigoDeErro.ACESSO_NEGADO, negado(Principal.de(c.usuarioFree()), outro));
    }

    @Test
    @DisplayName("RN-18 · CT-18.02 · premium vigente em dados de outro usuário → acesso_negado")
    void rn18_ct18_02_premiumEmOutroUsuario() {
        UUID outro = c.usuarioFree();

        assertEquals(CodigoDeErro.ACESSO_NEGADO, negado(Principal.de(c.usuarioPremiumVigente()), outro));
    }

    @Test
    @DisplayName("RN-18 · CT-18.03 · anônimo em dados de outro usuário → 401")
    void rn18_ct18_03_anonimoEmOutroUsuario() {
        UUID outro = c.usuarioFree();

        assertTrue(FAMILIA_401.contains(negado(Principal.anonimo(), outro)));
    }

    @Test
    @Disabled("Bloqueado: QT-13, usuário inexistente: 403 ou 404?")
    @DisplayName("RN-18 · CT-18.04 · identificador de usuário inexistente")
    void rn18_ct18_04_usuarioInexistente() {
    }
}
