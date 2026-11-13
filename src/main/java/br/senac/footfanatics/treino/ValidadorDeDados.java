package br.senac.footfanatics.treino;

import java.util.List;

/** Esquema, qualidade e quarentena (§6.3 item 8; ADR-007). */
public class ValidadorDeDados {

    public ValidadorDeDados(ConfiguracaoDoJob configuracao) {
    }

    public ResultadoDaValidacao validar(List<EventoDeEntrada> eventos) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }
}
