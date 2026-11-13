package br.senac.footfanatics.apoio;

import br.senac.footfanatics.tempo.Relogio;
import java.time.Duration;
import java.time.Instant;

public class RelogioAjustavel implements Relogio {
    private volatile Instant atual;

    public RelogioAjustavel(Instant inicio) {
        this.atual = inicio;
    }

    @Override
    public Instant agora() {
        return atual;
    }

    public void definir(Instant instante) {
        this.atual = instante;
    }

    public void avancar(Duration duracao) {
        this.atual = atual.plus(duracao);
    }
}
