package br.senac.footfanatics.apoio;

import br.senac.footfanatics.treino.Avaliacao;
import br.senac.footfanatics.treino.AvaliadorDeCandidato;
import br.senac.footfanatics.treino.CandidatoTreinado;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

/** Substitui as métricas do Modelo M (pendentes, Q-09). */
public class AvaliadorFalso implements AvaliadorDeCandidato {
    public Avaliacao resposta = new Avaliacao(true, 0.90, OptionalDouble.of(0.80));

    @Override
    public Avaliacao avaliar(CandidatoTreinado candidato, Optional<UUID> ativa) {
        return resposta;
    }
}
