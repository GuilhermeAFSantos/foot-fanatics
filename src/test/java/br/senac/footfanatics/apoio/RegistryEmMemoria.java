package br.senac.footfanatics.apoio;

import br.senac.footfanatics.treino.ExecucaoDeTreino;
import br.senac.footfanatics.treino.RegistryDeModelos;
import br.senac.footfanatics.treino.StatusExecucao;
import br.senac.footfanatics.treino.StatusVersao;
import br.senac.footfanatics.treino.VersaoDoModelo;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Armazenamento que segue o contrato documentado de RegistryDeModelos (finalizar = transação única).
 * Conta escritas para os testes de "sem efeito".
 */
public class RegistryEmMemoria implements RegistryDeModelos {
    private final Map<UUID, ExecucaoDeTreino> execucoes = new LinkedHashMap<>();
    private final Map<UUID, VersaoDoModelo> versoes = new LinkedHashMap<>();
    private Instant watermark;
    private UUID ativa;
    private UUID anterior;
    public int escritas;

    public RegistryEmMemoria(Instant watermarkInicial) {
        this.watermark = watermarkInicial;
    }

    @Override
    public Optional<ExecucaoDeTreino> buscarExecucao(LocalDate data) {
        return execucoes.values().stream().filter(e -> e.dataReferencia().equals(data)).findFirst();
    }

    @Override
    public List<ExecucaoDeTreino> execucoes() {
        return new ArrayList<>(execucoes.values());
    }

    @Override
    public void salvarExecucao(ExecucaoDeTreino e) {
        escritas++;
        execucoes.put(e.id(), e);
    }

    @Override
    public Instant watermark() {
        return watermark;
    }

    @Override
    public void registrarVersao(VersaoDoModelo v) {
        escritas++;
        versoes.put(v.id(), v);
    }

    @Override
    public void atualizarStatusDaVersao(UUID id, StatusVersao s) {
        escritas++;
        VersaoDoModelo v = versoes.get(id);
        versoes.put(id, new VersaoDoModelo(v.id(), s, v.execucaoId(), v.baseVersaoId(),
                v.hashArtefato(), v.versaoCodigo(), v.parametros()));
    }

    @Override
    public List<VersaoDoModelo> versoes() {
        return new ArrayList<>(versoes.values());
    }

    @Override
    public Optional<UUID> versaoAtiva() {
        return Optional.ofNullable(ativa);
    }

    @Override
    public Optional<UUID> versaoAnterior() {
        return Optional.ofNullable(anterior);
    }

    @Override
    public void finalizar(UUID execucaoId, StatusExecucao statusFinal, Instant novoWatermark, UUID versaoPromovida) {
        escritas++;
        ExecucaoDeTreino e = execucoes.get(execucaoId);
        execucoes.put(execucaoId, new ExecucaoDeTreino(e.id(), e.dataReferencia(), statusFinal,
                e.watermarkInicio(), e.corte(), e.hashDados()));
        if (novoWatermark != null) {
            watermark = novoWatermark;
        }
        if (versaoPromovida != null) {
            anterior = ativa;
            ativa = versaoPromovida;
        }
    }

    @Override
    public void trocarPonteiro(UUID nova, UUID anteriorNova) {
        escritas++;
        ativa = nova;
        anterior = anteriorNova;
    }

    // ---- Arrange (não conta como escrita do sistema) ----

    public void semearAtiva(VersaoDoModelo v) {
        versoes.put(v.id(), v);
        ativa = v.id();
    }

    public void semear(VersaoDoModelo v) {
        versoes.put(v.id(), v);
    }

    public void semear(ExecucaoDeTreino e) {
        execucoes.put(e.id(), e);
    }

    public void semearPonteiro(UUID ativaSemeada, UUID anteriorSemeada) {
        ativa = ativaSemeada;
        anterior = anteriorSemeada;
    }

    public VersaoDoModelo versao(UUID id) {
        return versoes.get(id);
    }
}
