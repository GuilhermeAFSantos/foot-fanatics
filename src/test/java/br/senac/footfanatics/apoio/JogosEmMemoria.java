package br.senac.footfanatics.apoio;

import br.senac.footfanatics.esportes.JogoExterno;
import br.senac.footfanatics.esportes.RepositorioDeJogos;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class JogosEmMemoria implements RepositorioDeJogos {
    private final Map<Long, JogoExterno> dados = new ConcurrentHashMap<>();

    @Override
    public void salvar(JogoExterno jogo) {
        dados.put(jogo.idExterno(), jogo);
    }

    @Override
    public Optional<JogoExterno> buscarPorIdExterno(long idExterno) {
        return Optional.ofNullable(dados.get(idExterno));
    }

    public int quantidade() {
        return dados.size();
    }
}
