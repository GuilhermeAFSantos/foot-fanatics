package br.senac.footfanatics.apoio;

import static br.senac.footfanatics.apoio.ValoresDeTeste.LIMITE_QUARENTENA;
import static br.senac.footfanatics.apoio.ValoresDeTeste.MARGEM;
import static br.senac.footfanatics.apoio.ValoresDeTeste.MAX_REPROVACOES;
import static br.senac.footfanatics.apoio.ValoresDeTeste.MAX_TENTATIVAS;
import static br.senac.footfanatics.apoio.ValoresDeTeste.PISO;
import static br.senac.footfanatics.apoio.ValoresDeTeste.T0;
import static br.senac.footfanatics.apoio.ValoresDeTeste.TOLERANCIA_GATE;
import static br.senac.footfanatics.apoio.ValoresDeTeste.VERSAO_ESQUEMA_ATUAL;

import br.senac.footfanatics.treino.ConfiguracaoDoJob;
import br.senac.footfanatics.treino.EventoDeEntrada;
import br.senac.footfanatics.treino.ExtratorIncremental;
import br.senac.footfanatics.treino.GateDePromocao;
import br.senac.footfanatics.treino.OrquestradorDoJob;
import br.senac.footfanatics.treino.StatusVersao;
import br.senac.footfanatics.treino.ValidadorDeDados;
import br.senac.footfanatics.treino.VersaoDoModelo;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** Monta o Job de Treino Diário com dublês. Só Arrange, sem regra. */
public class CenarioDoJob {
    public static final LocalDate DIA = LocalDate.of(2026, 11, 13);
    /** Watermark inicial: um dia antes de T0. */
    public static final Instant W0 = T0.minus(Duration.ofDays(1));

    public final ConfiguracaoDoJob configuracao = new ConfiguracaoDoJob(MARGEM, MAX_REPROVACOES, MAX_TENTATIVAS,
            LIMITE_QUARENTENA, VERSAO_ESQUEMA_ATUAL, TOLERANCIA_GATE, PISO);
    public final RelogioAjustavel relogio = new RelogioAjustavel(T0);
    public final EventosEmMemoria eventos = new EventosEmMemoria();
    public final RegistryEmMemoria registry = new RegistryEmMemoria(W0);
    public final LockFalso lock = new LockFalso();
    public final TreinadorFalso treinador = new TreinadorFalso();
    public final AvaliadorFalso avaliador = new AvaliadorFalso();
    public final ExtratorIncremental extrator = new ExtratorIncremental(eventos);
    public final ValidadorDeDados validador = new ValidadorDeDados(configuracao);
    public final GateDePromocao gate = new GateDePromocao(configuracao);
    public final OrquestradorDoJob orquestrador = new OrquestradorDoJob(registry, lock, extrator, validador,
            treinador, avaliador, gate, relogio, configuracao);

    private final AtomicLong seq = new AtomicLong();

    public EventoDeEntrada evento(Instant registradoEm) {
        return evento(registradoEm, VERSAO_ESQUEMA_ATUAL);
    }

    public EventoDeEntrada evento(Instant registradoEm, int versaoEsquema) {
        return new EventoDeEntrada(seq.incrementAndGet(), UUID.randomUUID(), "uso", versaoEsquema,
                "sujeito-" + seq.get(), registradoEm, registradoEm);
    }

    /** Grava diretamente no dublê (simula evento já persistido). */
    public EventoDeEntrada gravar(Instant registradoEm) {
        EventoDeEntrada e = evento(registradoEm);
        eventos.gravar(e);
        return e;
    }

    /** Semeia uma versão ATIVA e devolve o id. */
    public UUID versaoAtiva() {
        UUID id = UUID.randomUUID();
        registry.semearAtiva(new VersaoDoModelo(id, StatusVersao.ATIVO, null, null, "hash-ativo", "codigo-0", "semente=1"));
        return id;
    }
}
