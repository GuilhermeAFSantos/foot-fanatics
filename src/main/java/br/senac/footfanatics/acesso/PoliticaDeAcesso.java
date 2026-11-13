package br.senac.footfanatics.acesso;

import br.senac.footfanatics.assinatura.Plano;
import br.senac.footfanatics.assinatura.ServicoDeAssinatura;
import br.senac.footfanatics.erro.CodigoDeErro;
import br.senac.footfanatics.erro.FalhaDeAcesso;
import java.util.Optional;
import java.util.UUID;

/** Decisão única de acesso, nega por padrão (ADR-002; matriz docs/arquitetura.md §4.1). */
public class PoliticaDeAcesso {

    private final ServicoDeAssinatura assinaturas;

    public PoliticaDeAcesso(ServicoDeAssinatura assinaturas) {
        this.assinaturas = assinaturas;
    }

    /** Lança FalhaDeAcesso quando o principal não pode ver conteúdo com esta classificação. */
    public void exigirAcessoAoConteudo(Principal principal, Classificacao classificacao) {
        if (classificacao == Classificacao.PUBLICA) {
            return;
        }
        if (classificacao != Classificacao.EXCLUSIVA) {
            throw new FalhaDeAcesso(CodigoDeErro.ACESSO_NEGADO); // sem regra explícita: nega (RN-19)
        }
        if (anonimo(principal)) {
            throw new FalhaDeAcesso(CodigoDeErro.AUTENTICACAO_NECESSARIA);
        }
        Optional<Plano> plano = assinaturas.planoVigente(principal.usuarioId());
        if (plano.orElse(null) != Plano.PREMIUM) {
            throw new FalhaDeAcesso(CodigoDeErro.PLANO_INSUFICIENTE);
        }
    }

    /** Lança FalhaDeAcesso quando o principal não pode ver dados de conta/sessão/assinatura do dono. */
    public void exigirAcessoAosDadosDe(Principal principal, UUID dono) {
        if (anonimo(principal)) {
            throw new FalhaDeAcesso(CodigoDeErro.AUTENTICACAO_NECESSARIA);
        }
        if (dono == null || !dono.equals(principal.usuarioId())) {
            throw new FalhaDeAcesso(CodigoDeErro.ACESSO_NEGADO);
        }
    }

    private static boolean anonimo(Principal principal) {
        return principal == null || principal.usuarioId() == null;
    }
}
