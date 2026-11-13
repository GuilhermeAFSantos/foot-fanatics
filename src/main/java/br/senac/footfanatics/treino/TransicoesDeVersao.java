package br.senac.footfanatics.treino;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Transições permitidas de docs/arquitetura.md §6.4; qualquer outra é proibida. */
public final class TransicoesDeVersao {

    private static final Map<StatusVersao, Set<StatusVersao>> PERMITIDAS = Map.of(
            StatusVersao.CANDIDATO, EnumSet.of(StatusVersao.REPROVADO, StatusVersao.ATIVO, StatusVersao.ARQUIVADO),
            StatusVersao.ATIVO, EnumSet.of(StatusVersao.APROVADO_INATIVO),
            StatusVersao.APROVADO_INATIVO, EnumSet.of(StatusVersao.ATIVO, StatusVersao.ARQUIVADO),
            StatusVersao.REPROVADO, EnumSet.of(StatusVersao.ARQUIVADO),
            StatusVersao.ARQUIVADO, EnumSet.noneOf(StatusVersao.class));

    private TransicoesDeVersao() {
    }

    public static void exigirPermitida(StatusVersao de, StatusVersao para) {
        if (de == null || para == null || !PERMITIDAS.get(de).contains(para)) {
            throw new TransicaoProibidaException("transição proibida: " + de + " → " + para);
        }
    }
}
