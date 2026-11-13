package br.senac.footfanatics.acesso;

import br.senac.footfanatics.assinatura.ServicoDeAssinatura;
import java.util.UUID;

/** Decisão única de acesso, nega por padrão (ADR-002). */
public class PoliticaDeAcesso {

    public PoliticaDeAcesso(ServicoDeAssinatura assinaturas) {
    }

    /** Lança FalhaDeAcesso quando o principal não pode ver conteúdo com esta classificação. */
    public void exigirAcessoAoConteudo(Principal principal, Classificacao classificacao) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    /** Lança FalhaDeAcesso quando o principal não pode ver dados de conta/sessão/assinatura do dono. */
    public void exigirAcessoAosDadosDe(Principal principal, UUID dono) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }
}
