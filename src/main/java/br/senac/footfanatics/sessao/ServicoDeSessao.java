package br.senac.footfanatics.sessao;

import br.senac.footfanatics.acesso.Principal;
import br.senac.footfanatics.erro.CodigoDeErro;
import br.senac.footfanatics.erro.FalhaDeAcesso;
import br.senac.footfanatics.identidade.AutenticadorDeCredenciais;
import br.senac.footfanatics.identidade.RevogadorDeSessoes;
import br.senac.footfanatics.seguranca.Hashes;
import br.senac.footfanatics.tempo.Relogio;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Login, validação, renovação e logout (E2; ADR-004, docs/arquitetura.md §4.2).
 *
 * <p>Tokens opacos no formato {@code idDaSessao.versao.assinatura}, assinados com HMAC por uma chave do servidor.
 * A SESSAO guarda só hashes. O "mesmo par" devolvido na tolerância de reuso é recalculado a partir da versão,
 * sem guardar texto puro. A chave é gerada por instância: em produção deve vir de configuração secreta.
 */
public class ServicoDeSessao implements RevogadorDeSessoes {

    private static final String ACESSO = "A";
    private static final String REFRESH = "R";

    private final AutenticadorDeCredenciais autenticador;
    private final RepositorioDeSessoes sessoes;
    private final Relogio relogio;
    private final ConfiguracaoDeSessao configuracao;
    private final byte[] chave = Hashes.bytesAleatorios(32);

    public ServicoDeSessao(AutenticadorDeCredenciais autenticador,
                           RepositorioDeSessoes sessoes,
                           Relogio relogio,
                           ConfiguracaoDeSessao configuracao) {
        this.autenticador = autenticador;
        this.sessoes = sessoes;
        this.relogio = relogio;
        this.configuracao = configuracao;
    }

    public ParDeTokens login(String email, String senha, String dispositivo) {
        UUID usuario = autenticador.autenticar(email, senha).orElseThrow(CredenciaisInvalidasException::new);
        Instant agora = relogio.agora();
        UUID id = UUID.randomUUID();
        int versao = 0;
        sessoes.salvar(new Sessao(id, usuario, dispositivo,
                Hashes.sha256(token(ACESSO, id, versao)), agora.plus(configuracao.ttlAcesso()),
                Hashes.sha256(token(REFRESH, id, versao)), agora.plus(configuracao.ttlRefresh()),
                null, null, versao, null));
        return par(id, versao);
    }

    /**
     * Decisão do token de acesso, na ordem da tabela do cenário E2 (Aula 07):
     * ausente → TOKEN_AUSENTE; assinatura inválida (malformado, desconhecido ou de versão antiga) → TOKEN_INVALIDO;
     * expiração ≤ agora → TOKEN_EXPIRADO; sessão revogada → TOKEN_REVOGADO; válido e vigente → Principal.
     * Só um token válido e não expirado segue para as demais decisões de autorização.
     */
    public Principal validar(String tokenDeAcesso) {
        Sessao s = sessaoDoTokenDeAcesso(tokenDeAcesso);
        if (!relogio.agora().isBefore(s.acessoExpiraEm())) {
            throw new FalhaDeAcesso(CodigoDeErro.TOKEN_EXPIRADO);
        }
        if (s.revogadaEm() != null) {
            throw new FalhaDeAcesso(CodigoDeErro.TOKEN_REVOGADO);
        }
        return Principal.de(s.usuarioId());
    }

    public ParDeTokens renovar(String refreshToken, String dispositivo) {
        Optional<TokenLido> lido = ler(refreshToken);
        if (lido.isEmpty()) {
            throw invalido();
        }
        while (true) {
            Sessao s = sessoes.buscarPorId(lido.get().sessao()).orElseThrow(ServicoDeSessao::invalido);
            if (s.revogadaEm() != null) {
                throw invalido();
            }
            Instant agora = relogio.agora();
            String hash = Hashes.sha256(refreshToken);

            if (Hashes.iguais(hash, s.hashRefreshToken())) {
                if (agora.isAfter(s.refreshExpiraEm())) {
                    throw new FalhaDeAcesso(CodigoDeErro.TOKEN_EXPIRADO);
                }
                int nova = s.versao() + 1;
                Sessao rotacionada = new Sessao(s.id(), s.usuarioId(), s.dispositivo(),
                        Hashes.sha256(token(ACESSO, s.id(), nova)), agora.plus(configuracao.ttlAcesso()),
                        Hashes.sha256(token(REFRESH, s.id(), nova)), agora.plus(configuracao.ttlRefresh()),
                        s.hashRefreshToken(), agora, nova, null);
                if (sessoes.substituirSeVersao(rotacionada, s.versao())) {
                    return par(s.id(), nova);
                }
                continue; // outra renovação venceu: reavalia contra a sessão atualizada
            }

            if (Hashes.iguais(hash, s.hashRefreshAnterior())) {
                if (dentroDaTolerancia(s, dispositivo, agora)) {
                    return par(s.id(), s.versao());
                }
                revogar(s.id());
                throw invalido();
            }

            // Refresh mais antigo que o anterior: só revoga se for autêntico (evita revogação por token forjado).
            if (Hashes.iguais(refreshToken, token(REFRESH, s.id(), lido.get().versao()))) {
                revogar(s.id());
            }
            throw invalido();
        }
    }

    /**
     * Revoga a sessão do dispositivo atual. Token ausente, inválido ou de sessão já revogada → o mesmo código da
     * validação (provisório, QT-05). A expiração do token de acesso não impede o logout.
     */
    public void logout(String tokenDeAcesso) {
        Sessao s = sessaoDoTokenDeAcesso(tokenDeAcesso);
        if (s.revogadaEm() != null) {
            throw new FalhaDeAcesso(CodigoDeErro.TOKEN_REVOGADO);
        }
        revogar(s.id());
    }

    @Override
    public void revogarTodas(UUID usuarioId) {
        sessoes.listarTodas().stream()
                .filter(s -> s.usuarioId().equals(usuarioId) && s.revogadaEm() == null)
                .forEach(s -> revogar(s.id()));
    }

    // ------------------------------------------------------------------

    /** Sessão de um token de acesso autêntico (revogada ou não). Ausente → TOKEN_AUSENTE; não autêntico → TOKEN_INVALIDO. */
    private Sessao sessaoDoTokenDeAcesso(String tokenDeAcesso) {
        if (tokenDeAcesso == null || tokenDeAcesso.isBlank()) {
            throw new FalhaDeAcesso(CodigoDeErro.TOKEN_AUSENTE);
        }
        TokenLido lido = ler(tokenDeAcesso).orElseThrow(ServicoDeSessao::invalido);
        Sessao s = sessoes.buscarPorId(lido.sessao()).orElseThrow(ServicoDeSessao::invalido);
        if (!Hashes.iguais(Hashes.sha256(tokenDeAcesso), s.hashTokenDeAcesso())) {
            throw invalido();
        }
        return s;
    }

    private boolean dentroDaTolerancia(Sessao s, String dispositivo, Instant agora) {
        return s.renovadaEm() != null
                && s.dispositivo() != null
                && s.dispositivo().equals(dispositivo)
                && !agora.isAfter(s.renovadaEm().plus(configuracao.toleranciaDeReuso()));
    }

    /** Revoga com controle otimista (incrementa a versão), para não perder a revogação numa corrida. */
    private void revogar(UUID sessaoId) {
        while (true) {
            Optional<Sessao> atual = sessoes.buscarPorId(sessaoId);
            if (atual.isEmpty() || atual.get().revogadaEm() != null) {
                return;
            }
            Sessao s = atual.get();
            Sessao revogada = new Sessao(s.id(), s.usuarioId(), s.dispositivo(), s.hashTokenDeAcesso(),
                    s.acessoExpiraEm(), s.hashRefreshToken(), s.refreshExpiraEm(), s.hashRefreshAnterior(),
                    s.renovadaEm(), s.versao() + 1, relogio.agora());
            if (sessoes.substituirSeVersao(revogada, s.versao())) {
                return;
            }
        }
    }

    private ParDeTokens par(UUID sessao, int versao) {
        return new ParDeTokens(token(ACESSO, sessao, versao), token(REFRESH, sessao, versao));
    }

    private String token(String tipo, UUID sessao, int versao) {
        return sessao + "." + versao + "." + Hashes.hmac(chave, tipo + ":" + sessao + ":" + versao);
    }

    private static Optional<TokenLido> ler(String token) {
        if (token == null) {
            return Optional.empty();
        }
        String[] partes = token.split("\\.", -1);
        if (partes.length != 3 || partes[2].isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new TokenLido(UUID.fromString(partes[0]), Integer.parseInt(partes[1])));
        } catch (IllegalArgumentException malformado) {
            return Optional.empty();
        }
    }

    private static FalhaDeAcesso invalido() {
        return new FalhaDeAcesso(CodigoDeErro.TOKEN_INVALIDO);
    }

    private record TokenLido(UUID sessao, int versao) {
    }
}
