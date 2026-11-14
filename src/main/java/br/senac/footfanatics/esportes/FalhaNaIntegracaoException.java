package br.senac.footfanatics.esportes;

import java.util.UUID;

/**
 * Falha do provedor traduzida para o domínio. A mensagem é neutra e fixa: nunca carrega corpo, header,
 * URL com credencial nem a exceção original do provedor (que pode citar trechos da resposta).
 * A correlação liga esta falha ao log da chamada.
 */
public class FalhaNaIntegracaoException extends RuntimeException {

    /** Por que a integração falhou, do ponto de vista do Foot Fanatics. */
    public enum Motivo {
        /** O provedor respondeu 404: o jogo não existe lá. Permanente. */
        NAO_ENCONTRADO,
        /** 5xx, timeout ou falha de rede. Pode ser transitório. */
        INDISPONIVEL,
        /** Outro 4xx (credencial, request): permanente, não deve ser repetido. */
        RECUSADA,
        /** 2xx com conteúdo que não respeita o contrato declarado. */
        RESPOSTA_INVALIDA
    }

    private final Motivo motivo;
    private final UUID correlacao;

    public FalhaNaIntegracaoException(Motivo motivo, UUID correlacao) {
        super("falha na integração com o provedor de esportes: " + motivo + " (correlação " + correlacao + ")");
        this.motivo = motivo;
        this.correlacao = correlacao;
    }

    public Motivo motivo() {
        return motivo;
    }

    public UUID correlacao() {
        return correlacao;
    }
}
