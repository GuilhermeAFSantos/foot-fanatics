package br.senac.footfanatics.assinatura;

import java.util.List;
import java.util.UUID;

public interface RepositorioDeAssinaturas {
    void salvar(Assinatura assinatura);

    List<Assinatura> listarPorUsuario(UUID usuarioId);
}
