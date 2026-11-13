package br.senac.footfanatics.apoio;

import br.senac.footfanatics.treino.CandidatoTreinado;
import br.senac.footfanatics.treino.EventoDeEntrada;
import br.senac.footfanatics.treino.Treinador;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Substitui o algoritmo do Modelo M (pendente, Q-09). Registra as chamadas recebidas. */
public class TreinadorFalso implements Treinador {

    public record Chamada(Optional<UUID> base, List<EventoDeEntrada> dados) {
    }

    public final List<Chamada> chamadas = new ArrayList<>();
    public final Deque<RuntimeException> falhasProgramadas = new ArrayDeque<>();

    @Override
    public CandidatoTreinado treinar(Optional<UUID> base, List<EventoDeEntrada> dados) {
        chamadas.add(new Chamada(base, List.copyOf(dados)));
        if (!falhasProgramadas.isEmpty()) {
            throw falhasProgramadas.pop();
        }
        return new CandidatoTreinado("hash-" + chamadas.size(), "codigo-teste", "semente=1");
    }
}
