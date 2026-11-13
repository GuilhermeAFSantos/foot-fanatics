package br.senac.footfanatics.identidade;

import br.senac.footfanatics.tempo.Relogio;
import java.util.Optional;
import java.util.UUID;

/** Cadastro, credencial e recuperação de acesso (E1). */
public class ServicoDeIdentidade implements AutenticadorDeCredenciais {

    public ServicoDeIdentidade(RepositorioDeUsuarios usuarios,
                               RepositorioDeCodigosDeRecuperacao codigos,
                               CanalDeRecuperacao canal,
                               RevogadorDeSessoes revogador,
                               Relogio relogio,
                               ConfiguracaoDeRecuperacao configuracao) {
    }

    public UUID cadastrar(String email, String senha) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    @Override
    public Optional<UUID> autenticar(String email, String senha) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    public RespostaDeRecuperacao solicitarRecuperacao(String email) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    public void redefinirSenha(String codigo, String novaSenha) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }
}
