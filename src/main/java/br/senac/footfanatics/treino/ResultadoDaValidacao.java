package br.senac.footfanatics.treino;

import java.util.List;

public record ResultadoDaValidacao(boolean aprovado, List<EventoDeEntrada> validos,
                                   List<EventoDeEntrada> emQuarentena) {
}
