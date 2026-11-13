package br.senac.footfanatics.apoio;

import br.senac.footfanatics.assinatura.Assinatura;
import br.senac.footfanatics.assinatura.RepositorioDeAssinaturas;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AssinaturasEmMemoria implements RepositorioDeAssinaturas {
    private final Map<UUID, Assinatura> dados = new ConcurrentHashMap<>();

    @Override
    public void salvar(Assinatura a) {
        dados.put(a.id(), a);
    }

    @Override
    public List<Assinatura> listarPorUsuario(UUID usuarioId) {
        return dados.values().stream().filter(a -> a.usuarioId().equals(usuarioId)).toList();
    }
}
