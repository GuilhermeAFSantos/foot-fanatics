package br.senac.footfanatics.identidade;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioDeUsuarios {
    void salvar(Usuario usuario);

    Optional<Usuario> buscarPorEmail(String email);

    Optional<Usuario> buscarPorId(UUID id);

    List<Usuario> listarTodos();
}
