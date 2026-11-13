package br.senac.footfanatics.identidade;

import java.time.Duration;

/** Validade do código de recuperação: valor pendente (L-05). */
public record ConfiguracaoDeRecuperacao(Duration validadeDoCodigo) {
}
