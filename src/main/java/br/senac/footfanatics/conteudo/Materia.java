package br.senac.footfanatics.conteudo;

import br.senac.footfanatics.acesso.Classificacao;
import java.util.UUID;

public record Materia(UUID id, String titulo, String corpo, Classificacao classificacao) {
}
