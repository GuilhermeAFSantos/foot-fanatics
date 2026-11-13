package br.senac.footfanatics.apoio;

import br.senac.footfanatics.treino.EventoDeEntrada;
import br.senac.footfanatics.treino.RepositorioDeEventos;
import java.util.ArrayList;
import java.util.List;

/** Armazenamento puro, sem unicidade: a deduplicação é regra dos componentes, não do dublê. */
public class EventosEmMemoria implements RepositorioDeEventos {
    private final List<EventoDeEntrada> dados = new ArrayList<>();

    @Override
    public synchronized void gravar(EventoDeEntrada e) {
        dados.add(e);
    }

    @Override
    public synchronized List<EventoDeEntrada> lerTodos() {
        return new ArrayList<>(dados);
    }
}
