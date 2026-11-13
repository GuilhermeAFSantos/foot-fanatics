package br.senac.footfanatics.treino;

import java.time.Instant;
import java.util.List;

/** Lê a janela (watermark, corte], deduplicada (ADR-008, ADR-007). */
public class ExtratorIncremental {

    public ExtratorIncremental(RepositorioDeEventos repositorio) {
    }

    public List<EventoDeEntrada> extrair(Instant watermark, Instant corte) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }
}
