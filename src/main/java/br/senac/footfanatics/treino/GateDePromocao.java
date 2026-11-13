package br.senac.footfanatics.treino;

import java.util.OptionalDouble;

/** Critérios de promoção (§6.3 item 12; INV-3). */
public class GateDePromocao {

    private final ConfiguracaoDoJob configuracao;

    public GateDePromocao(ConfiguracaoDoJob configuracao) {
        this.configuracao = configuracao;
    }

    public boolean aprova(boolean dadosAprovados, boolean sanidadeAprovada,
                          double metricaCandidato, OptionalDouble metricaAtivo) {
        if (!dadosAprovados || !sanidadeAprovada) {
            return false;
        }
        if (metricaAtivo.isPresent()) {
            return metricaCandidato >= metricaAtivo.getAsDouble() - configuracao.toleranciaDoGate();
        }
        return metricaCandidato >= configuracao.piso();
    }
}
