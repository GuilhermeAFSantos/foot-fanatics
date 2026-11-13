package br.senac.footfanatics.conteudo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioDeConteudo {
    void salvar(Materia materia);

    Optional<Materia> buscarMateria(UUID id);

    List<Materia> listarMaterias();
}
