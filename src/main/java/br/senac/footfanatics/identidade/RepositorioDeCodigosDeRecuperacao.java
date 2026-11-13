package br.senac.footfanatics.identidade;

import java.util.List;
import java.util.Optional;

public interface RepositorioDeCodigosDeRecuperacao {
    void salvar(CodigoDeRecuperacao codigo);

    Optional<CodigoDeRecuperacao> buscarPorHash(String hashCodigo);

    List<CodigoDeRecuperacao> listarTodos();
}
