package br.senac.footfanatics.apoio;

import br.senac.footfanatics.identidade.AutenticadorDeCredenciais;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Credenciais conhecidas para os testes de sessão, que não dependem do cadastro (RN-01, bloqueado). */
public class AutenticadorFalso implements AutenticadorDeCredenciais {
    private final Map<String, UUID> contas = new HashMap<>();

    public UUID conhecer(String email, String senha) {
        UUID id = UUID.randomUUID();
        contas.put(email + "\u0000" + senha, id);
        return id;
    }

    @Override
    public Optional<UUID> autenticar(String email, String senha) {
        return Optional.ofNullable(contas.get(email + "\u0000" + senha));
    }
}
