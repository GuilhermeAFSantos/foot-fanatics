package br.senac.footfanatics.conteudo;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.senac.footfanatics.acesso.Classificacao;
import br.senac.footfanatics.acesso.PoliticaDeAcesso;
import br.senac.footfanatics.acesso.Principal;
import br.senac.footfanatics.apoio.CenarioDeAcesso;
import br.senac.footfanatics.erro.CodigoDeErro;
import br.senac.footfanatics.erro.FalhaDeAcesso;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/** E4 Conteúdo: RN-15, RN-16, RN-19, RN-20 (docs/arquitetura.md:605-606; ADR-002). */
class AcessoAoConteudoTest {

    /** Perfis da matriz §4.1. "Premium cancelado" usa vencido, porque t_corte do cancelamento está pendente (Q-03). */
    enum Perfil { ANONIMO, FREE, PREMIUM_VIGENTE, PREMIUM_VENCIDO }

    private CenarioDeAcesso c;

    @BeforeEach
    void montar() {
        c = new CenarioDeAcesso();
    }

    private Principal principal(Perfil perfil) {
        return switch (perfil) {
            case ANONIMO -> Principal.anonimo();
            case FREE -> Principal.de(c.usuarioFree());
            case PREMIUM_VIGENTE -> Principal.de(c.usuarioPremiumVigente());
            case PREMIUM_VENCIDO -> Principal.de(c.usuarioPremiumVencido());
        };
    }

    private CodigoDeErro negado(Principal p, Materia m) {
        return assertThrows(FalhaDeAcesso.class, () -> c.servicoDeConteudo.buscarMateria(p, m.id())).codigo();
    }

    // ---------------- RN-15 Conteúdo público (docs/arquitetura.md:605) ----------------

    @Test
    @DisplayName("RN-15 · CT-15.01 · anônimo lê conteúdo público")
    void rn15_ct15_01_anonimoLePublico() {
        Materia m = c.materia(Classificacao.PUBLICA);

        Materia lida = c.servicoDeConteudo.buscarMateria(Principal.anonimo(), m.id());

        assertEquals(m, lida);
    }

    @ParameterizedTest(name = "RN-15 · CT-15.02 · {0} lê conteúdo público")
    @EnumSource(value = Perfil.class, names = {"FREE", "PREMIUM_VIGENTE", "PREMIUM_VENCIDO"})
    void rn15_ct15_02_todosOsPerfisLeemPublico(Perfil perfil) {
        Materia m = c.materia(Classificacao.PUBLICA);

        Materia lida = c.servicoDeConteudo.buscarMateria(principal(perfil), m.id());

        assertEquals(m, lida);
    }

    @Test
    @Disabled("Bloqueado: QT-11, token inválido em rota pública dá 401 ou é tratado como anônimo?")
    @DisplayName("RN-15 · CT-15.03 · token inválido em conteúdo público")
    void rn15_ct15_03_tokenInvalidoEmPublico() {
    }

    // ---------------- RN-16 Conteúdo exclusivo (docs/arquitetura.md:606) ----------------

    @Test
    @DisplayName("RN-16 · CT-16.01 · premium vigente lê conteúdo exclusivo")
    void rn16_ct16_01_premiumVigenteLeExclusivo() {
        Materia m = c.materia(Classificacao.EXCLUSIVA);

        Materia lida = c.servicoDeConteudo.buscarMateria(principal(Perfil.PREMIUM_VIGENTE), m.id());

        assertEquals(m, lida);
    }

    @Test
    @DisplayName("RN-16 · CT-16.02 · anônimo em exclusivo → autenticacao_necessaria")
    void rn16_ct16_02_anonimoEmExclusivo() {
        Materia m = c.materia(Classificacao.EXCLUSIVA);

        assertEquals(CodigoDeErro.AUTENTICACAO_NECESSARIA, negado(Principal.anonimo(), m));
    }

    @Test
    @DisplayName("RN-16 · CT-16.03 · free em exclusivo → plano_insuficiente")
    void rn16_ct16_03_freeEmExclusivo() {
        Materia m = c.materia(Classificacao.EXCLUSIVA);

        assertEquals(CodigoDeErro.PLANO_INSUFICIENTE, negado(principal(Perfil.FREE), m));
    }

    @Test
    @DisplayName("RN-16 · CT-16.04 · premium vencido em exclusivo → plano_insuficiente")
    void rn16_ct16_04_premiumVencidoEmExclusivo() {
        Materia m = c.materia(Classificacao.EXCLUSIVA);

        assertEquals(CodigoDeErro.PLANO_INSUFICIENTE, negado(principal(Perfil.PREMIUM_VENCIDO), m));
    }

    @Test
    @DisplayName("RN-16 · CT-16.05 · negação nunca carrega campo do conteúdo exclusivo")
    void rn16_ct16_05_negacaoSemConteudo() {
        Materia m = c.materia(Classificacao.EXCLUSIVA);
        Principal free = principal(Perfil.FREE);

        FalhaDeAcesso falha = assertThrows(FalhaDeAcesso.class, () -> c.servicoDeConteudo.buscarMateria(free, m.id()));

        String tudo = String.valueOf(falha) + falha.getMessage() + falha.getCause();
        assertAll(
                () -> assertFalse(tudo.contains(m.titulo())),
                () -> assertFalse(tudo.contains(m.corpo())));
    }

    @Test
    @Disabled("Bloqueado: QT-12, em listas os exclusivos são omitidos ou listados sem corpo?")
    @DisplayName("RN-16 · CT-16.06 · lista mista pedida por free")
    void rn16_ct16_06_listaMista() {
    }

    @Test
    @Disabled("Bloqueado: Q-19, o que o free vê a mais que o anônimo")
    @DisplayName("RN-16 · CT-16.07 · diferença free × anônimo")
    void rn16_ct16_07_freeAlemDoAnonimo() {
    }

    // ---------------- RN-19 Negar por padrão (ADR-002) ----------------

    @Test
    @DisplayName("RN-19 · CT-19.01 · conteúdo sem classificação é negado até para premium vigente")
    void rn19_ct19_01_semClassificacaoNegado() {
        Materia m = new Materia(UUID.randomUUID(), "Sem classificação", "Corpo", null);
        c.conteudo.salvar(m);

        assertThrows(FalhaDeAcesso.class,
                () -> c.servicoDeConteudo.buscarMateria(principal(Perfil.PREMIUM_VIGENTE), m.id()));
    }

    /** Política que nega tudo: prova que todo caminho do ServicoDeConteudo passa por ela. */
    static class PoliticaQueNegaTudo extends PoliticaDeAcesso {
        PoliticaQueNegaTudo() {
            super(null);
        }

        @Override
        public void exigirAcessoAoConteudo(Principal principal, Classificacao classificacao) {
            throw new FalhaDeAcesso(CodigoDeErro.ACESSO_NEGADO);
        }

        @Override
        public void exigirAcessoAosDadosDe(Principal principal, UUID dono) {
            throw new FalhaDeAcesso(CodigoDeErro.ACESSO_NEGADO);
        }
    }

    @Test
    @DisplayName("RN-19 · CT-19.02 · nenhum caminho do ServicoDeConteudo devolve item sem a PoliticaDeAcesso")
    void rn19_ct19_02_todoCaminhoPassaPelaPolitica() {
        Materia publica = c.materia(Classificacao.PUBLICA);
        c.materia(Classificacao.EXCLUSIVA);
        ServicoDeConteudo servico = new ServicoDeConteudo(c.conteudo, new PoliticaQueNegaTudo());
        Principal premium = principal(Perfil.PREMIUM_VIGENTE);

        assertThrows(FalhaDeAcesso.class, () -> servico.buscarMateria(premium, publica.id()));
        List<Materia> listadas;
        try {
            listadas = servico.listarMaterias(premium);
        } catch (FalhaDeAcesso negado) {
            listadas = List.of();
        }
        assertTrue(listadas.isEmpty());
    }

    static Stream<Arguments> matriz() {
        return Stream.of(
                Arguments.of(Perfil.ANONIMO, Classificacao.PUBLICA, null),
                Arguments.of(Perfil.ANONIMO, Classificacao.EXCLUSIVA, CodigoDeErro.AUTENTICACAO_NECESSARIA),
                Arguments.of(Perfil.FREE, Classificacao.PUBLICA, null),
                Arguments.of(Perfil.FREE, Classificacao.EXCLUSIVA, CodigoDeErro.PLANO_INSUFICIENTE),
                Arguments.of(Perfil.PREMIUM_VIGENTE, Classificacao.PUBLICA, null),
                Arguments.of(Perfil.PREMIUM_VIGENTE, Classificacao.EXCLUSIVA, null),
                Arguments.of(Perfil.PREMIUM_VENCIDO, Classificacao.PUBLICA, null),
                Arguments.of(Perfil.PREMIUM_VENCIDO, Classificacao.EXCLUSIVA, CodigoDeErro.PLANO_INSUFICIENTE));
    }

    @ParameterizedTest(name = "RN-19 · CT-19.03 · {0} × {1} → {2} (nulo = permitido)")
    @MethodSource("matriz")
    void rn19_ct19_03_matrizCompleta(Perfil perfil, Classificacao classificacao, CodigoDeErro esperado) {
        Materia m = c.materia(classificacao);
        Principal p = principal(perfil);

        if (esperado == null) {
            assertEquals(m, c.servicoDeConteudo.buscarMateria(p, m.id()));
        } else {
            assertEquals(esperado, negado(p, m));
        }
    }

    // ---------------- RN-20 Domínio de conteúdo (README.md:3,12) ----------------

    @Test
    @Disabled("Sem regra: LG-03, nenhum artefato define regra sobre clubes, jogos, resultados ou matérias")
    @DisplayName("RN-20 · (sem caso) · regras do domínio de conteúdo")
    void rn20_semCasoPossivel() {
    }

    // ---------------- Aula 07: lacunas de risco apontadas pelo JaCoCo ----------------

    @Test
    @DisplayName("RN-19 · CT-19.05 · a listagem entrega ao premium o que a política libera e nunca o corpo exclusivo ao free")
    void rn19_ct19_05_listagemRespeitaAPolitica() {
        Materia publica = c.materia(Classificacao.PUBLICA);
        Materia exclusiva = c.materia(Classificacao.EXCLUSIVA);

        List<Materia> doPremium = c.servicoDeConteudo.listarMaterias(principal(Perfil.PREMIUM_VIGENTE));
        List<Materia> doFree = c.servicoDeConteudo.listarMaterias(principal(Perfil.FREE));

        // QT-12 (omitir × listar sem corpo) segue aberto: a asserção vale para as duas leituras.
        assertAll(
                () -> assertTrue(doPremium.containsAll(List.of(publica, exclusiva))),
                () -> assertTrue(doFree.contains(publica)),
                () -> assertTrue(doFree.stream().noneMatch(m -> exclusiva.corpo().equals(m.corpo()))));
    }
}
