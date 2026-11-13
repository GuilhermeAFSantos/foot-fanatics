package br.senac.footfanatics.treino;

import br.senac.footfanatics.tempo.Relogio;
import java.time.LocalDate;

/** Sequencia o Job de Treino Diário (docs/arquitetura.md §6.2). */
public class OrquestradorDoJob {

    public OrquestradorDoJob(RegistryDeModelos registry, GerenciadorDeLock lock,
                             ExtratorIncremental extrator, ValidadorDeDados validador,
                             Treinador treinador, AvaliadorDeCandidato avaliador,
                             GateDePromocao gate, Relogio relogio, ConfiguracaoDoJob configuracao) {
    }

    public ResultadoDoJob executar(LocalDate dataReferencia) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    /** Decisão humana auditada que marca a janela como DESCARTADA (ADR-011). */
    public void descartarJanela(LocalDate dataReferencia, String responsavel) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }
}
