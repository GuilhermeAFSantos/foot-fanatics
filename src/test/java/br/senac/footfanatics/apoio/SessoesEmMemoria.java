package br.senac.footfanatics.apoio;

import br.senac.footfanatics.sessao.RepositorioDeSessoes;
import br.senac.footfanatics.sessao.Sessao;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SessoesEmMemoria implements RepositorioDeSessoes {
    private final Map<UUID, Sessao> dados = new HashMap<>();

    @Override
    public synchronized void salvar(Sessao s) {
        dados.put(s.id(), s);
    }

    @Override
    public synchronized Optional<Sessao> buscarPorId(UUID id) {
        return Optional.ofNullable(dados.get(id));
    }

    @Override
    public synchronized List<Sessao> listarTodas() {
        return new ArrayList<>(dados.values());
    }

    @Override
    public synchronized boolean substituirSeVersao(Sessao nova, int versaoEsperada) {
        Sessao atual = dados.get(nova.id());
        if (atual == null || atual.versao() != versaoEsperada) {
            return false;
        }
        dados.put(nova.id(), nova);
        return true;
    }
}
