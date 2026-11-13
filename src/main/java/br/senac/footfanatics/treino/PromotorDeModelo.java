package br.senac.footfanatics.treino;

import java.util.Optional;
import java.util.UUID;

/**
 * Único componente que altera o ponteiro manualmente (rollback), e só para versão APROVADO_INATIVO
 * (INV-3; ADR-010). A auditoria da ação depende de um componente que ainda não existe (DIV-03).
 */
public class PromotorDeModelo {

    private final RegistryDeModelos registry;

    public PromotorDeModelo(RegistryDeModelos registry) {
        this.registry = registry;
    }

    /** Volta para a versão anterior. Sem versão anterior: proibido (provisório, QT-15). */
    public void rollback(String responsavel) {
        UUID anterior = registry.versaoAnterior()
                .orElseThrow(() -> new TransicaoProibidaException("não há versão anterior para rollback"));
        apontarPara(anterior, responsavel);
    }

    public void apontarPara(UUID versaoId, String responsavel) {
        if (responsavel == null || responsavel.isBlank()) {
            throw new IllegalArgumentException("ação manual sobre o ponteiro exige responsável identificado");
        }
        VersaoDoModelo alvo = registry.versoes().stream().filter(v -> v.id().equals(versaoId)).findFirst()
                .orElseThrow(() -> new TransicaoProibidaException("versão inexistente: " + versaoId));
        if (alvo.status() != StatusVersao.APROVADO_INATIVO) {
            throw new TransicaoProibidaException("o ponteiro só aponta manualmente para versão APROVADO_INATIVO");
        }
        Optional<UUID> atual = registry.versaoAtiva();
        TransicoesDeVersao.exigirPermitida(alvo.status(), StatusVersao.ATIVO);
        atual.ifPresent(id -> {
            TransicoesDeVersao.exigirPermitida(StatusVersao.ATIVO, StatusVersao.APROVADO_INATIVO);
            registry.atualizarStatusDaVersao(id, StatusVersao.APROVADO_INATIVO);
        });
        registry.atualizarStatusDaVersao(alvo.id(), StatusVersao.ATIVO);
        registry.trocarPonteiro(alvo.id(), atual.orElse(null));
    }
}
