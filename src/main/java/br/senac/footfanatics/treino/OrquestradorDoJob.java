package br.senac.footfanatics.treino;

import br.senac.footfanatics.seguranca.Hashes;
import br.senac.footfanatics.tempo.Relogio;
import br.senac.footfanatics.treino.ResultadoDoJob.Desfecho;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Sequencia o Job de Treino Diário (docs/arquitetura.md §6.1–6.4; ADR-008 a ADR-012).
 *
 * <ul>
 *   <li>Lock indisponível → sem efeito (ADR-009).</li>
 *   <li>Data já em estado terminal → sem efeito; data FALHOU → retoma com a mesma janela (§6.3 item 3).</li>
 *   <li>K reprovações consecutivas → para e aguarda decisão humana (ADR-011).</li>
 *   <li>Checkpoint e ponteiro só mudam em {@link RegistryDeModelos#finalizar} (INV-2, INV-3; ADR-010).</li>
 * </ul>
 */
public class OrquestradorDoJob {

    private static final Set<StatusExecucao> TERMINAIS =
            Set.of(StatusExecucao.CONCLUIDA, StatusExecucao.REPROVADA, StatusExecucao.DESCARTADA);

    private final RegistryDeModelos registry;
    private final GerenciadorDeLock lock;
    private final ExtratorIncremental extrator;
    private final ValidadorDeDados validador;
    private final Treinador treinador;
    private final AvaliadorDeCandidato avaliador;
    private final GateDePromocao gate;
    private final Relogio relogio;
    private final ConfiguracaoDoJob configuracao;

    public OrquestradorDoJob(RegistryDeModelos registry, GerenciadorDeLock lock,
                             ExtratorIncremental extrator, ValidadorDeDados validador,
                             Treinador treinador, AvaliadorDeCandidato avaliador,
                             GateDePromocao gate, Relogio relogio, ConfiguracaoDoJob configuracao) {
        this.registry = registry;
        this.lock = lock;
        this.extrator = extrator;
        this.validador = validador;
        this.treinador = treinador;
        this.avaliador = avaliador;
        this.gate = gate;
        this.relogio = relogio;
        this.configuracao = configuracao;
    }

    public ResultadoDoJob executar(LocalDate dataReferencia) {
        if (!lock.tentarAdquirir()) {
            return new ResultadoDoJob(Desfecho.IGNORADA_LOCK_INDISPONIVEL, false);
        }
        try {
            return executarComLock(dataReferencia);
        } finally {
            lock.liberar();
        }
    }

    /**
     * Decisão humana auditada que marca a janela como DESCARTADA e avança o checkpoint (ADR-011).
     * Só vale para a execução REPROVADA mais recente, depois de K reprovações consecutivas.
     */
    public void descartarJanela(LocalDate dataReferencia, String responsavel) {
        if (responsavel == null || responsavel.isBlank()) {
            throw new DescarteNaoPermitidoException("o descarte exige responsável identificado");
        }
        ExecucaoDeTreino exec = registry.buscarExecucao(dataReferencia)
                .orElseThrow(() -> new DescarteNaoPermitidoException("execução inexistente: " + dataReferencia));
        Optional<ExecucaoDeTreino> maisRecente = execucoesPorData().stream().reduce((a, b) -> b);
        if (exec.status() != StatusExecucao.REPROVADA
                || maisRecente.isEmpty() || !maisRecente.get().id().equals(exec.id())) {
            throw new DescarteNaoPermitidoException("só a execução REPROVADA mais recente pode ser descartada");
        }
        if (reprovacoesConsecutivas() < configuracao.maxReprovacoesConsecutivas()) {
            throw new DescarteNaoPermitidoException("descarte só depois de K reprovações consecutivas");
        }
        registry.finalizar(exec.id(), StatusExecucao.DESCARTADA, exec.corte(), null);
    }

    // ------------------------------------------------------------------

    private ResultadoDoJob executarComLock(LocalDate data) {
        Optional<ExecucaoDeTreino> existente = registry.buscarExecucao(data);
        if (existente.isPresent() && TERMINAIS.contains(existente.get().status())) {
            return new ResultadoDoJob(Desfecho.SEM_EFEITO_JA_CONCLUIDA, false);
        }
        if (reprovacoesConsecutivas() >= configuracao.maxReprovacoesConsecutivas()) {
            return new ResultadoDoJob(Desfecho.PARADA_AGUARDANDO_DECISAO, false);
        }
        arquivarOrfaos();

        ExecucaoDeTreino base = existente.orElseGet(() -> new ExecucaoDeTreino(UUID.randomUUID(), data,
                StatusExecucao.EM_EXECUCAO, registry.watermark(), relogio.agora().minus(configuracao.margem()), null));
        List<EventoDeEntrada> janela = extrator.extrair(base.watermarkInicio(), base.corte());
        ExecucaoDeTreino exec = new ExecucaoDeTreino(base.id(), data, StatusExecucao.EM_EXECUCAO,
                base.watermarkInicio(), base.corte(), hashDosDados(janela));
        registry.salvarExecucao(exec);

        try {
            ResultadoDaValidacao validacao = validador.validar(janela);
            if (!validacao.aprovado()) {
                return falhou(exec);
            }
            Optional<UUID> ativa = registry.versaoAtiva();
            CandidatoTreinado candidato = treinarComNovasTentativas(ativa, validacao.validos());
            VersaoDoModelo versao = new VersaoDoModelo(UUID.randomUUID(), StatusVersao.CANDIDATO, exec.id(),
                    ativa.orElse(null), candidato.hashArtefato(), candidato.versaoCodigo(), candidato.parametros());
            registry.registrarVersao(versao);

            Avaliacao avaliacao = avaliador.avaliar(candidato, ativa);
            boolean aprovado = gate.aprova(true, avaliacao.sanidadeAprovada(), avaliacao.metricaCandidato(),
                    avaliacao.metricaAtivo());
            if (aprovado) {
                promover(exec, versao, ativa);
                return new ResultadoDoJob(Desfecho.CONCLUIDA, false);
            }
            mudarStatus(versao.id(), StatusVersao.CANDIDATO, StatusVersao.REPROVADO);
            registry.finalizar(exec.id(), StatusExecucao.REPROVADA, null, null);
            boolean alerta = reprovacoesConsecutivas() >= configuracao.maxReprovacoesConsecutivas();
            return new ResultadoDoJob(Desfecho.REPROVADA, alerta);
        } catch (RuntimeException erro) {
            return falhou(exec);
        }
    }

    private void promover(ExecucaoDeTreino exec, VersaoDoModelo versao, Optional<UUID> ativa) {
        ativa.ifPresent(id -> mudarStatus(id, StatusVersao.ATIVO, StatusVersao.APROVADO_INATIVO));
        mudarStatus(versao.id(), StatusVersao.CANDIDATO, StatusVersao.ATIVO);
        registry.finalizar(exec.id(), StatusExecucao.CONCLUIDA, exec.corte(), versao.id());
    }

    private CandidatoTreinado treinarComNovasTentativas(Optional<UUID> base, List<EventoDeEntrada> dados) {
        ErroTransitorio ultimo = null;
        for (int tentativa = 1; tentativa <= configuracao.maxTentativas(); tentativa++) {
            try {
                return treinador.treinar(base, dados);
            } catch (ErroTransitorio transitorio) {
                ultimo = transitorio; // erro de dados (ou qualquer outro) não é repetido
            }
        }
        throw ultimo != null ? ultimo : new IllegalStateException("maxTentativas deve ser ≥ 1");
    }

    private ResultadoDoJob falhou(ExecucaoDeTreino exec) {
        registry.salvarExecucao(new ExecucaoDeTreino(exec.id(), exec.dataReferencia(), StatusExecucao.FALHOU,
                exec.watermarkInicio(), exec.corte(), exec.hashDados()));
        return new ResultadoDoJob(Desfecho.FALHOU, false);
    }

    /** Candidatos de execuções FALHOU que nunca passaram pelo gate (§6.3 item 6; §6.4). */
    private void arquivarOrfaos() {
        Set<UUID> falhas = registry.execucoes().stream()
                .filter(e -> e.status() == StatusExecucao.FALHOU)
                .map(ExecucaoDeTreino::id)
                .collect(Collectors.toSet());
        registry.versoes().stream()
                .filter(v -> v.status() == StatusVersao.CANDIDATO && falhas.contains(v.execucaoId()))
                .forEach(v -> mudarStatus(v.id(), StatusVersao.CANDIDATO, StatusVersao.ARQUIVADO));
    }

    /** Reprovações desde a última execução CONCLUIDA ou DESCARTADA (QT-14 lido como "consecutivas"). */
    private int reprovacoesConsecutivas() {
        int contagem = 0;
        List<ExecucaoDeTreino> porData = execucoesPorData();
        for (int i = porData.size() - 1; i >= 0; i--) {
            StatusExecucao s = porData.get(i).status();
            if (s == StatusExecucao.REPROVADA) {
                contagem++;
            } else if (s == StatusExecucao.CONCLUIDA || s == StatusExecucao.DESCARTADA) {
                break;
            }
        }
        return contagem;
    }

    private List<ExecucaoDeTreino> execucoesPorData() {
        return registry.execucoes().stream()
                .sorted(Comparator.comparing(ExecucaoDeTreino::dataReferencia))
                .toList();
    }

    private void mudarStatus(UUID versao, StatusVersao de, StatusVersao para) {
        TransicoesDeVersao.exigirPermitida(de, para);
        registry.atualizarStatusDaVersao(versao, para);
    }

    private static String hashDosDados(List<EventoDeEntrada> janela) {
        String ids = janela.stream().map(e -> e.idEvento().toString()).sorted().collect(Collectors.joining(","));
        return Hashes.sha256(ids);
    }
}
