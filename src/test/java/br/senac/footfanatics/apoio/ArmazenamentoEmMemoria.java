package br.senac.footfanatics.apoio;

import br.senac.footfanatics.treino.ArmazenamentoDeArtefatos;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ArmazenamentoEmMemoria implements ArmazenamentoDeArtefatos {
    public final Map<String, byte[]> dados = new HashMap<>();

    @Override
    public String gravar(byte[] artefato) {
        String hash = "h" + dados.size();
        dados.put(hash, artefato);
        return hash;
    }

    @Override
    public Optional<byte[]> ler(String hash) {
        return Optional.ofNullable(dados.get(hash));
    }
}
