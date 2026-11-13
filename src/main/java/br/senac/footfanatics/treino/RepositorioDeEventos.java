package br.senac.footfanatics.treino;

import java.util.List;

public interface RepositorioDeEventos {
    void gravar(EventoDeEntrada evento);

    List<EventoDeEntrada> lerTodos();
}
