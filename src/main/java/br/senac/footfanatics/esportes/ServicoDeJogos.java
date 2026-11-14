package br.senac.footfanatics.esportes;

/**
 * Importa um jogo do provedor. Só persiste depois que a porta devolveu um jogo válido:
 * timeout, erro HTTP ou corpo inválido não deixam nenhum efeito colateral.
 */
public class ServicoDeJogos {

    private final SportsProvider provedor;
    private final RepositorioDeJogos jogos;

    public ServicoDeJogos(SportsProvider provedor, RepositorioDeJogos jogos) {
        this.provedor = provedor;
        this.jogos = jogos;
    }

    public JogoExterno sincronizar(long idExterno) {
        JogoExterno jogo = provedor.buscarJogo(idExterno);
        jogos.salvar(jogo);
        return jogo;
    }
}
