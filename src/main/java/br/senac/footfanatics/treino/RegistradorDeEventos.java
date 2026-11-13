package br.senac.footfanatics.treino;

/** Grava eventos de entrada append-only; o mesmo id_evento é gravado uma única vez (ADR-007). */
public class RegistradorDeEventos {

    private final RepositorioDeEventos repositorio;

    public RegistradorDeEventos(RepositorioDeEventos repositorio) {
        this.repositorio = repositorio;
    }

    public synchronized void registrar(EventoDeEntrada evento) {
        boolean jaGravado = repositorio.lerTodos().stream().anyMatch(e -> e.idEvento().equals(evento.idEvento()));
        if (!jaGravado) {
            repositorio.gravar(evento);
        }
    }
}
