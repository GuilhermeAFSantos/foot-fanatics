package br.senac.footfanatics.apoio;

import br.senac.footfanatics.conteudo.Materia;
import br.senac.footfanatics.conteudo.RepositorioDeConteudo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ConteudoEmMemoria implements RepositorioDeConteudo {
    private final Map<UUID, Materia> dados = new LinkedHashMap<>();

    @Override
    public void salvar(Materia m) {
        dados.put(m.id(), m);
    }

    @Override
    public Optional<Materia> buscarMateria(UUID id) {
        return Optional.ofNullable(dados.get(id));
    }

    @Override
    public List<Materia> listarMaterias() {
        return new ArrayList<>(dados.values());
    }
}
