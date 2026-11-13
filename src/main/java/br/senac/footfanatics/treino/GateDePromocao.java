package br.senac.footfanatics.treino;

import java.util.OptionalDouble;

/** Critérios de promoção (§6.3 item 12). */
public class GateDePromocao {

    public GateDePromocao(ConfiguracaoDoJob configuracao) {
    }

    public boolean aprova(boolean dadosAprovados, boolean sanidadeAprovada,
                          double metricaCandidato, OptionalDouble metricaAtivo) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }
}
