package br.senac.footfanatics.treino;

import java.util.ArrayList;
import java.util.List;

/**
 * Esquema, qualidade e quarentena (§6.3 item 8; ADR-007): aceita os esquemas N e N−1.
 * Reprova quando a taxa de quarentena fica ACIMA do limite. Igual ao limite aprova, pela leitura literal
 * de "acima" (provisório: AM-02). Janela vazia aprova (o volume mínimo está pendente).
 */
public class ValidadorDeDados {

    private final ConfiguracaoDoJob configuracao;

    public ValidadorDeDados(ConfiguracaoDoJob configuracao) {
        this.configuracao = configuracao;
    }

    public ResultadoDaValidacao validar(List<EventoDeEntrada> eventos) {
        List<EventoDeEntrada> validos = new ArrayList<>();
        List<EventoDeEntrada> quarentena = new ArrayList<>();
        int atual = configuracao.versaoDeEsquemaAtual();
        for (EventoDeEntrada e : eventos) {
            boolean esquemaAceito = e.versaoEsquema() == atual || e.versaoEsquema() == atual - 1;
            (esquemaAceito ? validos : quarentena).add(e);
        }
        double taxa = eventos.isEmpty() ? 0.0 : (double) quarentena.size() / eventos.size();
        return new ResultadoDaValidacao(taxa <= configuracao.limiteDeQuarentena(), List.copyOf(validos),
                List.copyOf(quarentena));
    }
}
