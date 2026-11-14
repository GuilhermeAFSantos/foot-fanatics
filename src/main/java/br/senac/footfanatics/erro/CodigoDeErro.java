package br.senac.footfanatics.erro;

/** Códigos de erro do contrato (docs/arquitetura.md §4.1/§4.2; ADR-014). */
public enum CodigoDeErro {
    AUTENTICACAO_NECESSARIA,
    PLANO_INSUFICIENTE,
    ACESSO_NEGADO,
    /** Requisição sem token de acesso (Aula 07, cenário E2). */
    TOKEN_AUSENTE,
    TOKEN_EXPIRADO,
    TOKEN_INVALIDO,
    /** Token autêntico de uma sessão revogada (Aula 07, cenário E2). */
    TOKEN_REVOGADO,
    /** O provedor de esportes não tem o jogo pedido (Aula 08, INT-02). */
    JOGO_NAO_ENCONTRADO,
    /** Provedor de esportes fora do ar, lento ou recusando a chamada (Aula 08, INT-03/INT-04). */
    INTEGRACAO_INDISPONIVEL,
    /** O provedor respondeu fora do contrato declarado (Aula 08, INT-05). */
    RESPOSTA_EXTERNA_INVALIDA
}
