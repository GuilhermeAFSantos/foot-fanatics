package br.senac.footfanatics.apoio;

import br.senac.footfanatics.identidade.RepositorioDeUsuarios;
import br.senac.footfanatics.identidade.Usuario;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UsuariosEmMemoria implements RepositorioDeUsuarios {
    private final Map<UUID, Usuario> dados = new ConcurrentHashMap<>();

    @Override
    public void salvar(Usuario u) {
        dados.put(u.id(), u);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return dados.values().stream().filter(u -> u.email().equals(email)).findFirst();
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return Optional.ofNullable(dados.get(id));
    }

    @Override
    public List<Usuario> listarTodos() {
        return new ArrayList<>(dados.values());
    }
}
