package br.senac.footfanatics.erro;

/** Códigos de erro do contrato (docs/arquitetura.md §4.1/§4.2; ADR-014). */
public enum CodigoDeErro {
    AUTENTICACAO_NECESSARIA,
    PLANO_INSUFICIENTE,
    ACESSO_NEGADO,
    TOKEN_EXPIRADO,
    TOKEN_INVALIDO
}
