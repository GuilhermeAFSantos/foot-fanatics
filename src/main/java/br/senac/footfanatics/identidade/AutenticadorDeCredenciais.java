package br.senac.footfanatics.identidade;

import java.util.Optional;
import java.util.UUID;

public interface AutenticadorDeCredenciais {
    /** Id do usuário quando as credenciais conferem; vazio caso contrário. */
    Optional<UUID> autenticar(String email, String senha);
}
