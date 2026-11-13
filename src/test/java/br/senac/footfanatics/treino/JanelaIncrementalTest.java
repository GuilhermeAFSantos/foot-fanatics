package br.senac.footfanatics.treino;

import static br.senac.footfanatics.apoio.ValoresDeTeste.T0;
import static br.senac.footfanatics.apoio.ValoresDeTeste.UM_MS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.senac.footfanatics.apoio.CenarioDoJob;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RS-01 (janela (watermark, corte]) e RS-04 (deduplicação): docs/arquitetura.md:792,795,798; ADR-007, ADR-008. */
class JanelaIncrementalTest {

    private static final Instant W = T0;
    private static final Instant CORTE = T0.plus(Duration.ofHours(1));

    private CenarioDoJob c;

    @BeforeEach
    void montar() {
        c = new CenarioDoJob();
    }

    private boolean naJanela(EventoDeEntrada e) {
        return c.extrator.extrair(W, CORTE).stream().anyMatch(x -> x.idEvento().equals(e.idEvento()));
    }

    // ---------------- RS-01 ----------------

    @Test
    @DisplayName("RS-01 · CT-S01.01 · eventos entre watermark e corte entram na janela")
    void rs01_cts01_01_eventosDentroDaJanela() {
        EventoDeEntrada a = c.gravar(W.plus(Duration.ofMinutes(1)));
        EventoDeEntrada b = c.gravar(W.plus(Duration.ofMinutes(30)));

        List<EventoDeEntrada> janela = c.extrator.extrair(W, CORTE);

        assertEquals(2, janela.size());
        assertTrue(janela.contains(a) && janela.contains(b));
    }

    @Test
    @DisplayName("RS-01 · CT-S01.02 · evento em registrado_em = watermark fica fora (aberto à esquerda)")
    void rs01_cts01_02_noWatermarkFora() {
        EventoDeEntrada e = c.gravar(W);

        assertFalse(naJanela(e));
    }

    @Test
    @DisplayName("RS-01 · CT-S01.03 · evento em watermark + 1 ms entra")
    void rs01_cts01_03_logoAposWatermarkDentro() {
        EventoDeEntrada e = c.gravar(W.plus(UM_MS));

        assertTrue(naJanela(e));
    }

    @Test
    @DisplayName("RS-01 · CT-S01.04 · evento em registrado_em = corte entra (fechado à direita)")
    void rs01_cts01_04_noCorteDentro() {
        EventoDeEntrada e = c.gravar(CORTE);

        assertTrue(naJanela(e));
    }

    @Test
    @DisplayName("RS-01 · CT-S01.05 · evento em corte + 1 ms fica para a próxima janela")
    void rs01_cts01_05_aposCorteFora() {
        EventoDeEntrada e = c.gravar(CORTE.plus(UM_MS));

        assertFalse(naJanela(e));
    }

    @Test
    @DisplayName("RS-01 · CT-S01.06 · evento tardio (ocorrido antigo, registrado novo) entra pelo tempo de ingestão")
    void rs01_cts01_06_eventoTardioEntra() {
        EventoDeEntrada tardio = new EventoDeEntrada(99, UUID.randomUUID(), "uso", 3, "s",
                W.minus(Duration.ofDays(10)), W.plus(Duration.ofMinutes(1)));
        c.eventos.gravar(tardio);

        assertTrue(naJanela(tardio));
    }

    // ---------------- RS-04 ----------------

    @Test
    @DisplayName("RS-04 · CT-S04.01 · o mesmo id_evento registrado duas vezes é gravado uma vez")
    void rs04_cts04_01_registradorNaoDuplica() {
        RegistradorDeEventos registrador = new RegistradorDeEventos(c.eventos);
        EventoDeEntrada e = c.evento(W.plus(Duration.ofMinutes(1)));

        registrador.registrar(e);
        registrador.registrar(e);

        assertEquals(1, c.eventos.lerTodos().size());
    }

    @Test
    @DisplayName("RS-04 · CT-S04.02 · id_evento repetido na janela é contado uma vez")
    void rs04_cts04_02_extratorDeduplica() {
        EventoDeEntrada e = c.evento(W.plus(Duration.ofMinutes(1)));
        c.eventos.gravar(e);
        c.eventos.gravar(e);

        List<EventoDeEntrada> janela = c.extrator.extrair(W, CORTE);

        assertEquals(1, janela.size());
    }

    @Test
    @Disabled("Bloqueado: Q-09 / RA-02, chave de negócio de deduplicação depende do Modelo M")
    @DisplayName("RS-04 · CT-S04.03 · mesmo fato com id_evento diferente")
    void rs04_cts04_03_duplicataDeNegocio() {
    }
}
