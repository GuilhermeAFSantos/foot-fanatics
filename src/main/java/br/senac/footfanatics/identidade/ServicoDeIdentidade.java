package br.senac.footfanatics.identidade;

import br.senac.footfanatics.seguranca.Hashes;
import br.senac.footfanatics.tempo.Relogio;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Cadastro, credencial e recuperação de acesso (E1; §4.2; ADR-015). */
public class ServicoDeIdentidade implements AutenticadorDeCredenciais {

    /** Mesma resposta para e-mail existente ou inexistente (evita enumeração de contas). */
    static final RespostaDeRecuperacao RESPOSTA_PADRAO =
            new RespostaDeRecuperacao("Se o e-mail estiver cadastrado, enviaremos as instruções de recuperação.");

    private final RepositorioDeUsuarios usuarios;
    private final RepositorioDeCodigosDeRecuperacao codigos;
    private final CanalDeRecuperacao canal;
    private final RevogadorDeSessoes revogador;
    private final Relogio relogio;
    private final ConfiguracaoDeRecuperacao configuracao;
    private final PasswordEncoder senhas = new BCryptPasswordEncoder();

    public ServicoDeIdentidade(RepositorioDeUsuarios usuarios,
                               RepositorioDeCodigosDeRecuperacao codigos,
                               CanalDeRecuperacao canal,
                               RevogadorDeSessoes revogador,
                               Relogio relogio,
                               ConfiguracaoDeRecuperacao configuracao) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.canal = canal;
        this.revogador = revogador;
        this.relogio = relogio;
        this.configuracao = configuracao;
    }

    /** Só e-mail e senha: os demais campos e a unicidade do e-mail estão pendentes (LG-01, QT-01). */
    public UUID cadastrar(String email, String senha) {
        UUID id = UUID.randomUUID();
        usuarios.salvar(new Usuario(id, email, senhas.encode(senha)));
        return id;
    }

    @Override
    public Optional<UUID> autenticar(String email, String senha) {
        if (email == null || senha == null) {
            return Optional.empty();
        }
        return usuarios.buscarPorEmail(email)
                .filter(u -> senhas.matches(senha, u.hashSenha()))
                .map(Usuario::id);
    }

    public RespostaDeRecuperacao solicitarRecuperacao(String email) {
        usuarios.buscarPorEmail(email).ifPresent(usuario -> {
            String codigo = Hashes.segredoAleatorio();
            codigos.salvar(new CodigoDeRecuperacao(UUID.randomUUID(), usuario.id(), Hashes.sha256(codigo),
                    relogio.agora(), null));
            canal.enviar(usuario.email(), codigo);
        });
        return RESPOSTA_PADRAO;
    }

    /**
     * Código de uso único, válido até emitidoEm + validade (borda provisória: o instante exato ainda vale, AM-01).
     * O sucesso revoga todas as sessões do titular.
     */
    public void redefinirSenha(String codigo, String novaSenha) {
        if (codigo == null) {
            throw new CodigoDeRecuperacaoInvalidoException();
        }
        Instant agora = relogio.agora();
        CodigoDeRecuperacao registro = codigos.buscarPorHash(Hashes.sha256(codigo))
                .orElseThrow(CodigoDeRecuperacaoInvalidoException::new);
        boolean expirado = agora.isAfter(registro.emitidoEm().plus(configuracao.validadeDoCodigo()));
        if (registro.usadoEm() != null || expirado) {
            throw new CodigoDeRecuperacaoInvalidoException();
        }
        Usuario usuario = usuarios.buscarPorId(registro.usuarioId())
                .orElseThrow(CodigoDeRecuperacaoInvalidoException::new);
        codigos.salvar(new CodigoDeRecuperacao(registro.id(), registro.usuarioId(), registro.hashCodigo(),
                registro.emitidoEm(), agora));
        usuarios.salvar(new Usuario(usuario.id(), usuario.email(), senhas.encode(novaSenha)));
        revogador.revogarTodas(usuario.id());
    }
}
