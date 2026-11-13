package br.senac.footfanatics.assinatura;

import br.senac.footfanatics.tempo.Relogio;
import java.util.Optional;
import java.util.UUID;

/** Planos, vigência, upgrade e cancelamento (E3). */
public class ServicoDeAssinatura {

    public ServicoDeAssinatura(RepositorioDeAssinaturas repositorio, Relogio relogio) {
    }

    /** Plano vigente agora; vazio = nenhum. */
    public Optional<Plano> planoVigente(UUID usuarioId) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    public void cancelar(UUID usuarioId) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    public void upgrade(UUID usuarioId) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }
}
