package br.senac.footfanatics.treino.jdbc;

import br.senac.footfanatics.treino.ExecucaoDeTreino;
import br.senac.footfanatics.treino.RegistryDeModelos;
import br.senac.footfanatics.treino.StatusExecucao;
import br.senac.footfanatics.treino.StatusVersao;
import br.senac.footfanatics.treino.VersaoDoModelo;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

/** Adaptador PostgreSQL do schema ml (ADR-010). */
public class RegistryDeModelosJdbc implements RegistryDeModelos {

    public RegistryDeModelosJdbc(DataSource dataSource) {
    }

    @Override
    public Optional<ExecucaoDeTreino> buscarExecucao(LocalDate dataReferencia) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    @Override
    public List<ExecucaoDeTreino> execucoes() {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    @Override
    public void salvarExecucao(ExecucaoDeTreino execucao) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    @Override
    public Instant watermark() {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    @Override
    public void registrarVersao(VersaoDoModelo versao) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    @Override
    public void atualizarStatusDaVersao(UUID versaoId, StatusVersao status) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    @Override
    public List<VersaoDoModelo> versoes() {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    @Override
    public Optional<UUID> versaoAtiva() {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    @Override
    public Optional<UUID> versaoAnterior() {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    @Override
    public void finalizar(UUID execucaoId, StatusExecucao statusFinal, Instant novoWatermark, UUID versaoPromovida) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    @Override
    public void trocarPonteiro(UUID novaVersao, UUID versaoAnterior) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }
}
