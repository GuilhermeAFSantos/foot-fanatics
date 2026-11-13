package br.senac.footfanatics.apoio;

import br.senac.footfanatics.identidade.CodigoDeRecuperacao;
import br.senac.footfanatics.identidade.RepositorioDeCodigosDeRecuperacao;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CodigosEmMemoria implements RepositorioDeCodigosDeRecuperacao {
    private final Map<UUID, CodigoDeRecuperacao> dados = new ConcurrentHashMap<>();

    @Override
    public void salvar(CodigoDeRecuperacao c) {
        dados.put(c.id(), c);
    }

    @Override
    public Optional<CodigoDeRecuperacao> buscarPorHash(String hash) {
        return dados.values().stream().filter(c -> c.hashCodigo().equals(hash)).findFirst();
    }

    @Override
    public List<CodigoDeRecuperacao> listarTodos() {
        return new ArrayList<>(dados.values());
    }
}
