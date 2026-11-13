package br.senac.footfanatics.treino;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Lê a janela (watermark, corte] por tempo de ingestão, deduplicada por id_evento (ADR-008, ADR-007). */
public class ExtratorIncremental {

    private final RepositorioDeEventos repositorio;

    public ExtratorIncremental(RepositorioDeEventos repositorio) {
        this.repositorio = repositorio;
    }

    public List<EventoDeEntrada> extrair(Instant watermark, Instant corte) {
        Set<UUID> vistos = new HashSet<>();
        return repositorio.lerTodos().stream()
                .filter(e -> e.registradoEm().isAfter(watermark) && !e.registradoEm().isAfter(corte))
                .filter(e -> vistos.add(e.idEvento()))
                .toList();
    }
}
