package br.senac.footfanatics.esportes;

/**
 * Porta para o provedor externo de dados esportivos (Aula 08). O domínio só conhece esta interface;
 * o protocolo (HTTP, JSON, credencial) fica no adapter.
 *
 * <p>Contrato: devolve o jogo já mapeado e validado, ou lança {@link FalhaNaIntegracaoException} com um
 * motivo do próprio domínio. Nunca devolve dado parcial nem conteúdo do provedor.
 */
public interface SportsProvider {

    JogoExterno buscarJogo(long idExterno);
}
