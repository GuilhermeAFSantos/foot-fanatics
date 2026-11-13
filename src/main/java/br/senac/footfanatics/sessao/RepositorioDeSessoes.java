package br.senac.footfanatics.sessao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioDeSessoes {
    void salvar(Sessao sessao);

    Optional<Sessao> buscarPorId(UUID id);

    List<Sessao> listarTodas();

    /** Grava a nova sessão só se a versão atual for a esperada (controle otimista). */
    boolean substituirSeVersao(Sessao nova, int versaoEsperada);
}
