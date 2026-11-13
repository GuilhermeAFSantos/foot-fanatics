package br.senac.footfanatics.treino;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Registry de Modelos: schema ml (ADR-010). */
public interface RegistryDeModelos {
    Optional<ExecucaoDeTreino> buscarExecucao(LocalDate dataReferencia);

    List<ExecucaoDeTreino> execucoes();

    /** Cria ou atualiza uma execução em estado não final. */
    void salvarExecucao(ExecucaoDeTreino execucao);

    Instant watermark();

    void registrarVersao(VersaoDoModelo versao);

    void atualizarStatusDaVersao(UUID versaoId, StatusVersao status);

    List<VersaoDoModelo> versoes();

    Optional<UUID> versaoAtiva();

    Optional<UUID> versaoAnterior();

    /**
     * Transação final única: status final da execução, novo watermark (nulo = mantém)
     * e versão promovida (nula = ponteiro inalterado).
     */
    void finalizar(UUID execucaoId, StatusExecucao statusFinal, Instant novoWatermark, UUID versaoPromovida);

    /** Troca o ponteiro do modelo ativo. */
    void trocarPonteiro(UUID novaVersao, UUID versaoAnterior);
}
