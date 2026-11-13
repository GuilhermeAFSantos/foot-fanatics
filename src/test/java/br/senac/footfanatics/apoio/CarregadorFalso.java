package br.senac.footfanatics.apoio;

import br.senac.footfanatics.treino.ArtefatoInvalidoException;
import br.senac.footfanatics.treino.CarregadorDeModelo;
import br.senac.footfanatics.treino.ModeloCarregado;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Artefatos cujo conteúdo é "corrompido" falham ao carregar. */
public class CarregadorFalso implements CarregadorDeModelo {

    @Override
    public ModeloCarregado carregar(UUID versao, byte[] artefato) {
        if (new String(artefato, StandardCharsets.UTF_8).equals("corrompido")) {
            throw new ArtefatoInvalidoException("artefato inválido");
        }
        return new ModeloCarregado(versao);
    }
}
