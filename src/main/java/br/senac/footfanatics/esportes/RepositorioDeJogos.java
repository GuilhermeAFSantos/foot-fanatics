package br.senac.footfanatics.esportes;

import java.util.Optional;

/** Persistência dos jogos importados do provedor (adaptador de produção pendente, como os demais, DG-02). */
public interface RepositorioDeJogos {

    void salvar(JogoExterno jogo);

    Optional<JogoExterno> buscarPorIdExterno(long idExterno);
}
