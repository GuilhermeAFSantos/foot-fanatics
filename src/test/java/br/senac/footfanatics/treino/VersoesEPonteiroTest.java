package br.senac.footfanatics.treino;

import static br.senac.footfanatics.apoio.CenarioDoJob.W0;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.senac.footfanatics.apoio.RegistryEmMemoria;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

/** RS-11: ponteiro, rollback e transições de §6.4 (docs/arquitetura.md:804, 809-820; ADR-010). */
class VersoesEPonteiroTest {

    /** Transições permitidas, copiadas de §6.4. Tudo o que não está aqui é proibido. */
    private static final Set<List<StatusVersao>> PERMITIDAS = Set.of(
            List.of(StatusVersao.CANDIDATO, StatusVersao.REPROVADO),
            List.of(StatusVersao.CANDIDATO, StatusVersao.ATIVO),
            List.of(StatusVersao.CANDIDATO, StatusVersao.ARQUIVADO),
            List.of(StatusVersao.ATIVO, StatusVersao.APROVADO_INATIVO),
            List.of(StatusVersao.APROVADO_INATIVO, StatusVersao.ATIVO),
            List.of(StatusVersao.APROVADO_INATIVO, StatusVersao.ARQUIVADO),
            List.of(StatusVersao.REPROVADO, StatusVersao.ARQUIVADO));

    private RegistryEmMemoria registry;
    private PromotorDeModelo promotor;
    private UUID anterior;
    private UUID atual;

    private static VersaoDoModelo versao(StatusVersao status) {
        return new VersaoDoModelo(UUID.randomUUID(), status, UUID.randomUUID(), null, "hash", "codigo", "p");
    }

    @BeforeEach
    void montar() {
        registry = new RegistryEmMemoria(W0);
        promotor = new PromotorDeModelo(registry);
        VersaoDoModelo v1 = versao(StatusVersao.APROVADO_INATIVO);
        VersaoDoModelo v2 = versao(StatusVersao.ATIVO);
        registry.semear(v1);
        registry.semear(v2);
        registry.semearPonteiro(v2.id(), v1.id());
        anterior = v1.id();
        atual = v2.id();
    }

    @Test
    @DisplayName("RS-11 · CT-S11.01 · rollback aponta para a versão anterior e inverte os estados")
    void rs11_cts11_01_rollback() {
        promotor.rollback("operadora.plantao");

        assertAll(
                () -> assertEquals(Optional.of(anterior), registry.versaoAtiva()),
                () -> assertEquals(StatusVersao.ATIVO, registry.versao(anterior).status()),
                () -> assertEquals(StatusVersao.APROVADO_INATIVO, registry.versao(atual).status()));
    }

    @ParameterizedTest(name = "RS-11 · CT-S11.02 · ponteiro nunca aponta para versão {0}")
    @EnumSource(value = StatusVersao.class, names = {"CANDIDATO", "REPROVADO", "ARQUIVADO"})
    void rs11_cts11_02_ponteiroSoParaVersaoAprovada(StatusVersao status) {
        VersaoDoModelo proibida = versao(status);
        registry.semear(proibida);

        assertThrows(TransicaoProibidaException.class, () -> promotor.apontarPara(proibida.id(), "operadora.plantao"));
        assertEquals(Optional.of(atual), registry.versaoAtiva());
    }

    @Test
    @Disabled("Bloqueado: QT-15, rollback sem versão anterior é proibido ou volta para sem modelo?")
    @DisplayName("RS-11 · CT-S11.04 · rollback sem versão anterior")
    void rs11_cts11_04_rollbackSemAnterior() {
    }

    static Stream<Arguments> todasAsTransicoes() {
        List<Arguments> todas = new ArrayList<>();
        for (StatusVersao de : StatusVersao.values()) {
            for (StatusVersao para : StatusVersao.values()) {
                todas.add(Arguments.of(de, para, PERMITIDAS.contains(List.of(de, para))));
            }
        }
        return todas.stream();
    }

    @ParameterizedTest(name = "RS-11 · CT-S11.05 · {0} → {1} permitida? {2}")
    @MethodSource("todasAsTransicoes")
    void rs11_cts11_05_transicoesDeVersao(StatusVersao de, StatusVersao para, boolean permitida) {
        if (permitida) {
            assertDoesNotThrow(() -> TransicoesDeVersao.exigirPermitida(de, para));
        } else {
            assertThrows(TransicaoProibidaException.class, () -> TransicoesDeVersao.exigirPermitida(de, para));
        }
    }
}
