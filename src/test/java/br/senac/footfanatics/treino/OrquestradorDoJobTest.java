package br.senac.footfanatics.treino;

import static br.senac.footfanatics.apoio.CenarioDoJob.DIA;
import static br.senac.footfanatics.apoio.CenarioDoJob.W0;
import static br.senac.footfanatics.apoio.ValoresDeTeste.MARGEM;
import static br.senac.footfanatics.apoio.ValoresDeTeste.MAX_TENTATIVAS;
import static br.senac.footfanatics.apoio.ValoresDeTeste.T0;
import static br.senac.footfanatics.apoio.ValoresDeTeste.VERSAO_ESQUEMA_ATUAL;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.senac.footfanatics.apoio.CenarioDoJob;
import br.senac.footfanatics.apoio.TreinadorFalso;
import br.senac.footfanatics.treino.ResultadoDoJob.Desfecho;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

/** Job de Treino Diário (docs/arquitetura.md §6.1–6.4; ADR-008 a ADR-012). */
class OrquestradorDoJobTest {

    private static final Instant CORTE = T0.minus(MARGEM);

    private CenarioDoJob c;
    private UUID ativa;

    @BeforeEach
    void montar() {
        c = new CenarioDoJob();
        ativa = c.versaoAtiva();
        c.gravar(T0.minus(Duration.ofHours(2)));
    }

    private void avaliacaoPior() {
        c.avaliador.resposta = new Avaliacao(true, 0.10, OptionalDouble.of(0.80));
    }

    private void diaSeguinte(int dias) {
        c.relogio.definir(T0.plus(Duration.ofDays(dias)));
    }

    private Optional<VersaoDoModelo> versaoDaExecucao(java.time.LocalDate dia) {
        UUID exec = c.registry.buscarExecucao(dia).orElseThrow().id();
        return c.registry.versoes().stream().filter(v -> exec.equals(v.execucaoId())).findFirst();
    }

    // ---------------- RS-01 ----------------

    @Test
    @DisplayName("RS-01 · CT-S01.08 · corte da execução = início − Δ")
    void rs01_cts01_08_corteEInicioMenosMargem() {
        c.orquestrador.executar(DIA);

        assertEquals(CORTE, c.registry.buscarExecucao(DIA).orElseThrow().corte());
    }

    @Test
    @DisplayName("RS-01 · CT-S01.07 · evento consumido numa execução CONCLUIDA não reaparece na seguinte")
    void rs01_cts01_07_eventoConsumidoNaoReaparece() {
        c.orquestrador.executar(DIA);
        diaSeguinte(1);
        EventoDeEntrada novo = c.gravar(T0.plus(Duration.ofDays(1)).minus(Duration.ofHours(1)));

        c.orquestrador.executar(DIA.plusDays(1));

        assertEquals(java.util.List.of(novo), c.treinador.chamadas.get(1).dados());
    }

    // ---------------- RS-02 Checkpoint ----------------

    @Test
    @DisplayName("RS-02 · CT-S02.01 · execução CONCLUIDA avança o checkpoint até o corte")
    void rs02_cts02_01_concluidaAvancaCheckpoint() {
        ResultadoDoJob r = c.orquestrador.executar(DIA);

        assertEquals(Desfecho.CONCLUIDA, r.desfecho());
        assertEquals(CORTE, c.registry.watermark());
    }

    @Test
    @DisplayName("RS-02 · CT-S02.02 · execução FALHOU mantém o checkpoint")
    void rs02_cts02_02_falhaMantemCheckpoint() {
        c.treinador.falhasProgramadas.push(new ErroDeDados("dado inconsistente"));

        ResultadoDoJob r = c.orquestrador.executar(DIA);

        assertEquals(Desfecho.FALHOU, r.desfecho());
        assertEquals(W0, c.registry.watermark());
    }

    @Test
    @DisplayName("RS-02 · CT-S02.03 · execução REPROVADA mantém o checkpoint")
    void rs02_cts02_03_reprovadaMantemCheckpoint() {
        avaliacaoPior();

        ResultadoDoJob r = c.orquestrador.executar(DIA);

        assertEquals(Desfecho.REPROVADA, r.desfecho());
        assertEquals(W0, c.registry.watermark());
    }

    @Test
    @DisplayName("RS-02 · CT-S02.04 · janela DESCARTADA por decisão humana avança o checkpoint")
    void rs02_cts02_04_descarteAvancaCheckpoint() {
        avaliacaoPior();
        c.orquestrador.executar(DIA);
        diaSeguinte(1);
        c.orquestrador.executar(DIA.plusDays(1));
        Instant corteDaUltima = c.registry.buscarExecucao(DIA.plusDays(1)).orElseThrow().corte();

        c.orquestrador.descartarJanela(DIA.plusDays(1), "operadora.plantao");

        assertEquals(corteDaUltima, c.registry.watermark());
        assertEquals(StatusExecucao.DESCARTADA, c.registry.buscarExecucao(DIA.plusDays(1)).orElseThrow().status());
    }

    @Test
    @DisplayName("RS-02 · CT-S02.05 · o checkpoint só corresponde a execuções CONCLUIDA ou DESCARTADA")
    void rs02_cts02_05_checkpointSoDeExecucaoTerminal() {
        c.treinador.falhasProgramadas.push(new ErroDeDados("falha"));
        c.orquestrador.executar(DIA);
        diaSeguinte(1);
        avaliacaoPior();
        c.orquestrador.executar(DIA.plusDays(1));
        diaSeguinte(2);
        c.avaliador.resposta = new Avaliacao(true, 0.95, OptionalDouble.of(0.80));
        c.orquestrador.executar(DIA.plusDays(2));

        Instant watermark = c.registry.watermark();
        assertTrue(c.registry.execucoes().stream()
                .filter(e -> e.corte() != null && e.corte().equals(watermark))
                .allMatch(e -> e.status() == StatusExecucao.CONCLUIDA || e.status() == StatusExecucao.DESCARTADA));
        assertNotEquals(W0, watermark);
    }

    // ---------------- RS-03 Idempotência ----------------

    @Test
    @DisplayName("RS-03 · CT-S03.01 · reexecutar data CONCLUIDA não tem efeito")
    void rs03_cts03_01_concluidaSemEfeito() {
        c.registry.semear(new ExecucaoDeTreino(UUID.randomUUID(), DIA, StatusExecucao.CONCLUIDA, W0, CORTE, "h"));

        ResultadoDoJob r = c.orquestrador.executar(DIA);

        assertAll(
                () -> assertEquals(Desfecho.SEM_EFEITO_JA_CONCLUIDA, r.desfecho()),
                () -> assertEquals(0, c.registry.escritas),
                () -> assertTrue(c.treinador.chamadas.isEmpty()));
    }

    @Test
    @DisplayName("RS-03 · CT-S03.02 · data FALHOU é retomada com a mesma janela e o mesmo hash_dados")
    void rs03_cts03_02_retomadaComMesmaJanela() {
        c.treinador.falhasProgramadas.push(new ErroDeDados("falha"));
        c.orquestrador.executar(DIA);
        ExecucaoDeTreino primeira = c.registry.buscarExecucao(DIA).orElseThrow();
        c.gravar(T0.plus(Duration.ofMinutes(1)));
        c.relogio.definir(T0.plus(Duration.ofMinutes(10)));

        c.orquestrador.executar(DIA);

        ExecucaoDeTreino segunda = c.registry.buscarExecucao(DIA).orElseThrow();
        assertAll(
                () -> assertEquals(primeira.corte(), segunda.corte()),
                () -> assertEquals(primeira.hashDados(), segunda.hashDados()),
                () -> assertEquals(c.treinador.chamadas.get(0).dados(), c.treinador.chamadas.get(1).dados()));
    }

    // ---------------- RS-05 Lock ----------------

    @Test
    @DisplayName("RS-05 · CT-S05.01 · lock indisponível → encerra sem efeito")
    void rs05_cts05_01_lockIndisponivelSemEfeito() {
        c.lock.disponivel = false;

        ResultadoDoJob r = c.orquestrador.executar(DIA);

        assertAll(
                () -> assertEquals(Desfecho.IGNORADA_LOCK_INDISPONIVEL, r.desfecho()),
                () -> assertEquals(0, c.registry.escritas),
                () -> assertTrue(c.treinador.chamadas.isEmpty()));
    }

    // ---------------- RS-06 Falha e órfãos ----------------

    @Test
    @DisplayName("RS-06 · CT-S06.01 · falha após registrar o candidato mantém checkpoint e ponteiro")
    void rs06_cts06_01_falhaAposCandidato() {
        AvaliadorDeCandidato quebrado = (cand, at) -> {
            throw new ErroDeDados("avaliação impossível");
        };
        OrquestradorDoJob o = new OrquestradorDoJob(c.registry, c.lock, c.extrator, c.validador,
                c.treinador, quebrado, c.gate, c.relogio, c.configuracao);

        ResultadoDoJob r = o.executar(DIA);

        assertAll(
                () -> assertEquals(Desfecho.FALHOU, r.desfecho()),
                () -> assertEquals(W0, c.registry.watermark()),
                () -> assertEquals(Optional.of(ativa), c.registry.versaoAtiva()),
                () -> assertEquals(StatusVersao.CANDIDATO, versaoDaExecucao(DIA).orElseThrow().status()));
    }

    @Test
    @DisplayName("RS-06 · CT-S06.02 · candidato órfão é arquivado na execução seguinte")
    void rs06_cts06_02_orfaoArquivadoNaSeguinte() {
        AvaliadorDeCandidato quebrado = (cand, at) -> {
            throw new ErroDeDados("avaliação impossível");
        };
        new OrquestradorDoJob(c.registry, c.lock, c.extrator, c.validador, c.treinador, quebrado,
                c.gate, c.relogio, c.configuracao).executar(DIA);
        UUID orfa = versaoDaExecucao(DIA).orElseThrow().id();
        diaSeguinte(1);

        c.orquestrador.executar(DIA.plusDays(1));

        assertEquals(StatusVersao.ARQUIVADO, c.registry.versao(orfa).status());
    }

    @Test
    @DisplayName("RS-06 · CT-S06.03a · erro transitório abaixo do limite de tentativas → conclui")
    void rs06_cts06_03a_transitorioRecupera() {
        for (int i = 0; i < MAX_TENTATIVAS - 1; i++) {
            c.treinador.falhasProgramadas.push(new ErroTransitorio("conexão"));
        }

        ResultadoDoJob r = c.orquestrador.executar(DIA);

        assertEquals(Desfecho.CONCLUIDA, r.desfecho());
        assertEquals(MAX_TENTATIVAS, c.treinador.chamadas.size());
    }

    @Test
    @DisplayName("RS-06 · CT-S06.03b · erro transitório em todas as tentativas → FALHOU")
    void rs06_cts06_03b_transitorioEsgota() {
        for (int i = 0; i < MAX_TENTATIVAS + 2; i++) {
            c.treinador.falhasProgramadas.push(new ErroTransitorio("conexão"));
        }

        ResultadoDoJob r = c.orquestrador.executar(DIA);

        assertEquals(Desfecho.FALHOU, r.desfecho());
        assertEquals(MAX_TENTATIVAS, c.treinador.chamadas.size());
    }

    @Test
    @DisplayName("RS-06 · CT-S06.04 · erro de dados não é repetido")
    void rs06_cts06_04_erroDeDadosSemRetry() {
        c.treinador.falhasProgramadas.push(new ErroDeDados("dado inválido"));

        ResultadoDoJob r = c.orquestrador.executar(DIA);

        assertEquals(Desfecho.FALHOU, r.desfecho());
        assertEquals(1, c.treinador.chamadas.size());
    }

    // ---------------- RS-07 (no fluxo) ----------------

    @Test
    @DisplayName("RS-07 · CT-S07.04 · treino nunca consome evento de esquema desconhecido")
    void rs07_cts07_04_treinoSemEsquemaDesconhecido() {
        for (int i = 0; i < 19; i++) {
            c.gravar(T0.minus(Duration.ofHours(3)));
        }
        EventoDeEntrada desconhecido = c.evento(T0.minus(Duration.ofHours(3)), VERSAO_ESQUEMA_ATUAL + 7);
        c.eventos.gravar(desconhecido);

        c.orquestrador.executar(DIA);

        assertFalse(c.treinador.chamadas.get(0).dados().contains(desconhecido));
    }

    // ---------------- RS-09 Reprovação, K e descarte ----------------

    @Test
    @DisplayName("RS-09 · CT-S09.01 · gate reprova → versão REPROVADO, execução REPROVADA, ponteiro e checkpoint mantidos")
    void rs09_cts09_01_reprovacao() {
        avaliacaoPior();

        c.orquestrador.executar(DIA);

        assertAll(
                () -> assertEquals(StatusVersao.REPROVADO, versaoDaExecucao(DIA).orElseThrow().status()),
                () -> assertEquals(StatusExecucao.REPROVADA, c.registry.buscarExecucao(DIA).orElseThrow().status()),
                () -> assertEquals(Optional.of(ativa), c.registry.versaoAtiva()),
                () -> assertEquals(W0, c.registry.watermark()));
    }

    @Test
    @DisplayName("RS-09 · CT-S09.02 · após REPROVADA, a próxima treina sobre o ativo com a janela acumulada")
    void rs09_cts09_02_proximaUsaAtivoEJanelaAcumulada() {
        avaliacaoPior();
        c.orquestrador.executar(DIA);
        EventoDeEntrada daPrimeiraJanela = c.treinador.chamadas.get(0).dados().get(0);
        diaSeguinte(1);

        c.orquestrador.executar(DIA.plusDays(1));

        TreinadorFalso.Chamada segunda = c.treinador.chamadas.get(1);
        assertEquals(Optional.of(ativa), segunda.base());
        assertTrue(segunda.dados().contains(daPrimeiraJanela));
    }

    @Test
    @DisplayName("RS-09 · CT-S09.03 · reprovação K−1 continua; K alerta; depois para e aguarda decisão")
    void rs09_cts09_03_limiteK() {
        avaliacaoPior();

        ResultadoDoJob r1 = c.orquestrador.executar(DIA);
        diaSeguinte(1);
        ResultadoDoJob r2 = c.orquestrador.executar(DIA.plusDays(1));
        diaSeguinte(2);
        ResultadoDoJob r3 = c.orquestrador.executar(DIA.plusDays(2));

        assertAll(
                () -> assertFalse(r1.alertaEmitido()),
                () -> assertTrue(r2.alertaEmitido()),
                () -> assertEquals(Desfecho.PARADA_AGUARDANDO_DECISAO, r3.desfecho()),
                () -> assertEquals(2, c.treinador.chamadas.size()));
    }

    @Test
    @DisplayName("RS-09 · CT-S09.04a · descarte antes de K reprovações é proibido")
    void rs09_cts09_04a_descarteAntesDeK() {
        avaliacaoPior();
        c.orquestrador.executar(DIA);

        assertThrows(DescarteNaoPermitidoException.class,
                () -> c.orquestrador.descartarJanela(DIA, "operadora.plantao"));
        assertEquals(W0, c.registry.watermark());
    }

    @Test
    @DisplayName("RS-09 · CT-S09.04b · descarte sem responsável identificado é proibido")
    void rs09_cts09_04b_descarteSemResponsavel() {
        avaliacaoPior();
        c.orquestrador.executar(DIA);
        diaSeguinte(1);
        c.orquestrador.executar(DIA.plusDays(1));

        assertThrows(DescarteNaoPermitidoException.class, () -> c.orquestrador.descartarJanela(DIA.plusDays(1), " "));
        assertEquals(W0, c.registry.watermark());
    }

    @Test
    @Disabled("Bloqueado: QT-14, o contador de reprovações consecutivas zera numa aprovação?")
    @DisplayName("RS-09 · CT-S09.05 · contador zera após aprovação")
    void rs09_cts09_05_contadorZera() {
    }

    // ---------------- RS-10 Promoção atômica ----------------

    @Test
    @DisplayName("RS-10 · CT-S10.01 · gate aprova → ponteiro, execução e checkpoint mudam juntos")
    void rs10_cts10_01_promocao() {
        c.orquestrador.executar(DIA);

        VersaoDoModelo nova = versaoDaExecucao(DIA).orElseThrow();
        assertAll(
                () -> assertEquals(Optional.of(nova.id()), c.registry.versaoAtiva()),
                () -> assertEquals(StatusVersao.ATIVO, c.registry.versao(nova.id()).status()),
                () -> assertEquals(StatusVersao.APROVADO_INATIVO, c.registry.versao(ativa).status()),
                () -> assertEquals(StatusExecucao.CONCLUIDA, c.registry.buscarExecucao(DIA).orElseThrow().status()),
                () -> assertEquals(CORTE, c.registry.watermark()));
    }

    @Test
    @DisplayName("RS-10 · CT-S10.03 · ponteiro e checkpoint nunca mudam um sem o outro")
    void rs10_cts10_03_ponteiroECheckpointJuntos() {
        c.treinador.falhasProgramadas.push(new ErroDeDados("falha"));
        c.orquestrador.executar(DIA);
        boolean ponteiroMudouNaFalha = !c.registry.versaoAtiva().equals(Optional.of(ativa));
        boolean checkpointMudouNaFalha = !c.registry.watermark().equals(W0);

        diaSeguinte(1);
        c.orquestrador.executar(DIA.plusDays(1));
        boolean ponteiroMudouNoSucesso = !c.registry.versaoAtiva().equals(Optional.of(ativa));
        boolean checkpointMudouNoSucesso = !c.registry.watermark().equals(W0);

        assertAll(
                () -> assertEquals(ponteiroMudouNaFalha, checkpointMudouNaFalha),
                () -> assertFalse(ponteiroMudouNaFalha),
                () -> assertEquals(ponteiroMudouNoSucesso, checkpointMudouNoSucesso),
                () -> assertTrue(ponteiroMudouNoSucesso));
    }

    // ---------------- RS-14 Treino incremental ----------------

    @Test
    @DisplayName("RS-14 · CT-S14.01 · candidato é registrado com base no modelo ativo")
    void rs14_cts14_01_baseEOAtivo() {
        c.orquestrador.executar(DIA);

        assertEquals(Optional.of(ativa), c.treinador.chamadas.get(0).base());
        assertEquals(ativa, versaoDaExecucao(DIA).orElseThrow().baseVersaoId());
    }

    @Test
    @DisplayName("RS-14 · CT-S14.02 · treino nunca lê eventos anteriores ou iguais ao watermark")
    void rs14_cts14_02_semDadosAntigos() {
        c.gravar(W0);
        c.gravar(W0.minus(Duration.ofHours(1)));

        c.orquestrador.executar(DIA);

        assertTrue(c.treinador.chamadas.get(0).dados().stream().allMatch(e -> e.registradoEm().isAfter(W0)));
    }

    // ---------------- Aula 07: lacunas de risco apontadas pelo JaCoCo ----------------

    /**
     * Janela de 10 eventos (1 do montar() + 9 aqui) e limite de quarentena 0,10: o limite é 1 evento.
     * Trinca −1/0/+1 em torno dele. No limite exato vale a leitura literal de "acima" (AM-02, provisória,
     * já usada em CT-S07.03c).
     */
    @ParameterizedTest(name = "quarentena = limite {0} → {1}")
    @CsvSource({"-1, CONCLUIDA", "0, CONCLUIDA", "1, FALHOU"})
    @DisplayName("RS-07 · CT-S07.06 · no Job, quarentena acima do limite → FALHOU sem treinar e sem mover o checkpoint")
    void rs07_cts07_06_quarentenaNoJob(int deslocamento, Desfecho esperado) {
        int emQuarentena = 1 + deslocamento;
        for (int i = 0; i < 9; i++) {
            int esquema = i < emQuarentena ? VERSAO_ESQUEMA_ATUAL + 1 : VERSAO_ESQUEMA_ATUAL;
            c.eventos.gravar(c.evento(T0.minus(Duration.ofHours(3)).minus(Duration.ofMinutes(i)), esquema));
        }

        ResultadoDoJob r = c.orquestrador.executar(DIA);

        boolean falhou = esperado == Desfecho.FALHOU;
        assertAll(
                () -> assertEquals(esperado, r.desfecho()),
                () -> assertEquals(falhou ? W0 : CORTE, c.registry.watermark()),
                () -> assertEquals(falhou ? 0 : 1, c.treinador.chamadas.size()),
                () -> assertEquals(falhou ? Optional.of(ativa) : Optional.of(versaoDaExecucao(DIA).orElseThrow().id()),
                        c.registry.versaoAtiva()));
    }

    @ParameterizedTest(name = "responsável [{0}] → descarte proibido")
    @NullAndEmptySource
    @DisplayName("RS-09 · CT-S09.04c · descarte com responsável nulo ou vazio é proibido")
    void rs09_cts09_04c_descarteComResponsavelNuloOuVazio(String responsavel) {
        avaliacaoPior();
        c.orquestrador.executar(DIA);
        diaSeguinte(1);
        c.orquestrador.executar(DIA.plusDays(1));

        assertThrows(DescarteNaoPermitidoException.class,
                () -> c.orquestrador.descartarJanela(DIA.plusDays(1), responsavel));
        assertEquals(W0, c.registry.watermark());
    }

    @Test
    @DisplayName("RS-09 · CT-S09.06 · só a execução REPROVADA mais recente pode ser descartada")
    void rs09_cts09_06_descarteSoDaMaisRecente() {
        avaliacaoPior();
        c.orquestrador.executar(DIA);
        diaSeguinte(1);
        c.orquestrador.executar(DIA.plusDays(1));

        assertThrows(DescarteNaoPermitidoException.class,
                () -> c.orquestrador.descartarJanela(DIA, "operadora.plantao"));
        assertAll(
                () -> assertEquals(W0, c.registry.watermark()),
                () -> assertEquals(StatusExecucao.REPROVADA, c.registry.buscarExecucao(DIA).orElseThrow().status()));
    }

    @Test
    @DisplayName("RS-09 · CT-S09.07a · descarte de data sem execução é proibido")
    void rs09_cts09_07a_descarteDeExecucaoInexistente() {
        assertThrows(DescarteNaoPermitidoException.class,
                () -> c.orquestrador.descartarJanela(DIA, "operadora.plantao"));
        assertAll(
                () -> assertEquals(W0, c.registry.watermark()),
                () -> assertTrue(c.registry.buscarExecucao(DIA).isEmpty()));
    }

    @Test
    @DisplayName("RS-09 · CT-S09.07b · descarte de execução CONCLUIDA é proibido")
    void rs09_cts09_07b_descarteDeExecucaoConcluida() {
        c.orquestrador.executar(DIA);

        assertThrows(DescarteNaoPermitidoException.class,
                () -> c.orquestrador.descartarJanela(DIA, "operadora.plantao"));
        assertAll(
                () -> assertEquals(CORTE, c.registry.watermark()),
                () -> assertEquals(StatusExecucao.CONCLUIDA, c.registry.buscarExecucao(DIA).orElseThrow().status()));
    }
}
