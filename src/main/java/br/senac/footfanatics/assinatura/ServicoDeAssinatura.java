package br.senac.footfanatics.assinatura;

import br.senac.footfanatics.tempo.Relogio;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Planos, vigência, upgrade e cancelamento (E3; ADR-003, ADR-005). */
public class ServicoDeAssinatura {

    private final RepositorioDeAssinaturas repositorio;
    private final Relogio relogio;

    public ServicoDeAssinatura(RepositorioDeAssinaturas repositorio, Relogio relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    /**
     * Plano vigente agora; vazio = nenhum. Premium vigente prevalece sobre free.
     * Borda provisória (AM-01 em aberto): o início conta como vigente e o fim não.
     */
    public Optional<Plano> planoVigente(UUID usuarioId) {
        Instant agora = relogio.agora();
        List<Assinatura> vigentes = repositorio.listarPorUsuario(usuarioId).stream()
                .filter(a -> vigenteEm(a, agora))
                .toList();
        if (vigentes.stream().anyMatch(a -> a.plano() == Plano.PREMIUM)) {
            return Optional.of(Plano.PREMIUM);
        }
        if (vigentes.stream().anyMatch(a -> a.plano() == Plano.FREE)) {
            return Optional.of(Plano.FREE);
        }
        return Optional.empty();
    }

    /**
     * Registra o cancelamento do premium vigente (canceladaEm = agora).
     * O instante em que o acesso é cortado depende de Q-03 (imediato ou fim da vigência) e ainda
     * NÃO é aplicado aqui: a vigência continua até fimVigencia. Sem premium vigente, não faz nada (QT-10).
     */
    public void cancelar(UUID usuarioId) {
        Instant agora = relogio.agora();
        repositorio.listarPorUsuario(usuarioId).stream()
                .filter(a -> a.plano() == Plano.PREMIUM && a.canceladaEm() == null && vigenteEm(a, agora))
                .forEach(a -> repositorio.salvar(new Assinatura(a.id(), a.usuarioId(), a.plano(),
                        a.inicioVigencia(), a.fimVigencia(), agora)));
    }

    /** Mecanismo do upgrade sem pagamento real é desconhecido (Q-02; S-04): nenhuma regra a implementar. */
    public void upgrade(UUID usuarioId) {
        throw new UnsupportedOperationException("upgrade sem regra definida: pendente Q-02");
    }

    private static boolean vigenteEm(Assinatura a, Instant agora) {
        boolean comecou = a.inicioVigencia() == null || !agora.isBefore(a.inicioVigencia());
        boolean naoTerminou = a.fimVigencia() == null || agora.isBefore(a.fimVigencia());
        return comecou && naoTerminou;
    }
}
