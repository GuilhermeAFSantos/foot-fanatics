package br.senac.footfanatics.sessao;

import br.senac.footfanatics.acesso.Principal;
import br.senac.footfanatics.identidade.AutenticadorDeCredenciais;
import br.senac.footfanatics.identidade.RevogadorDeSessoes;
import br.senac.footfanatics.tempo.Relogio;
import java.util.UUID;

/** Login, validação, renovação e logout (E2; ADR-004). */
public class ServicoDeSessao implements RevogadorDeSessoes {

    public ServicoDeSessao(AutenticadorDeCredenciais autenticador,
                           RepositorioDeSessoes sessoes,
                           Relogio relogio,
                           ConfiguracaoDeSessao configuracao) {
    }

    public ParDeTokens login(String email, String senha, String dispositivo) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    /** Principal autenticado ou FalhaDeAcesso (TOKEN_EXPIRADO / TOKEN_INVALIDO). */
    public Principal validar(String tokenDeAcesso) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    public ParDeTokens renovar(String refreshToken, String dispositivo) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    public void logout(String tokenDeAcesso) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    @Override
    public void revogarTodas(UUID usuarioId) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }
}
