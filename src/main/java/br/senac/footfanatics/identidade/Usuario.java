package br.senac.footfanatics.identidade;

import java.util.UUID;

public record Usuario(UUID id, String email, String hashSenha) {
}
