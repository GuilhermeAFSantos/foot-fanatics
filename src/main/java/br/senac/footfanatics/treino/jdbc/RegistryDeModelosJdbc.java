package br.senac.footfanatics.treino.jdbc;

import br.senac.footfanatics.treino.ExecucaoDeTreino;
import br.senac.footfanatics.treino.RegistryDeModelos;
import br.senac.footfanatics.treino.StatusExecucao;
import br.senac.footfanatics.treino.StatusVersao;
import br.senac.footfanatics.treino.VersaoDoModelo;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

/** Adaptador PostgreSQL do schema ml (ADR-010). {@link #finalizar} roda numa transação única. */
public class RegistryDeModelosJdbc implements RegistryDeModelos {

    private static final String JOB = "treino-diario";
    private static final String MODELO = "modelo-m";
    private static final String COLUNAS_EXECUCAO = "id, data_referencia, status, watermark_inicio, corte, hash_dados";
    private static final String COLUNAS_VERSAO = "id, status, execucao_id, base_versao_id, hash_artefato, versao_codigo, parametros";

    private final DataSource dataSource;

    public RegistryDeModelosJdbc(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<ExecucaoDeTreino> buscarExecucao(LocalDate dataReferencia) {
        return consultar("SELECT " + COLUNAS_EXECUCAO + " FROM ml.execucao_treino WHERE data_referencia = ?",
                ps -> ps.setDate(1, Date.valueOf(dataReferencia)), RegistryDeModelosJdbc::execucao).stream().findFirst();
    }

    @Override
    public List<ExecucaoDeTreino> execucoes() {
        return consultar("SELECT " + COLUNAS_EXECUCAO + " FROM ml.execucao_treino ORDER BY data_referencia",
                ps -> { }, RegistryDeModelosJdbc::execucao);
    }

    /** Cria ou atualiza uma execução não final. A unicidade de data_referencia é garantida pelo banco. */
    @Override
    public void salvarExecucao(ExecucaoDeTreino e) {
        executar("INSERT INTO ml.execucao_treino (" + COLUNAS_EXECUCAO + ") VALUES (?, ?, ?, ?, ?, ?)"
                + " ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, watermark_inicio = EXCLUDED.watermark_inicio,"
                + " corte = EXCLUDED.corte, hash_dados = EXCLUDED.hash_dados", ps -> {
                    ps.setObject(1, e.id());
                    ps.setDate(2, Date.valueOf(e.dataReferencia()));
                    ps.setString(3, e.status().name());
                    ps.setObject(4, tempo(e.watermarkInicio()));
                    ps.setObject(5, tempo(e.corte()));
                    ps.setString(6, e.hashDados());
                });
    }

    @Override
    public Instant watermark() {
        return consultar("SELECT watermark FROM ml.checkpoint_treino WHERE job = ?", ps -> ps.setString(1, JOB),
                rs -> instante(rs, "watermark")).stream().findFirst().orElse(null);
    }

    @Override
    public void registrarVersao(VersaoDoModelo v) {
        executar("INSERT INTO ml.versao_modelo (" + COLUNAS_VERSAO + ") VALUES (?, ?, ?, ?, ?, ?, ?)", ps -> {
            ps.setObject(1, v.id());
            ps.setString(2, v.status().name());
            ps.setObject(3, v.execucaoId());
            ps.setObject(4, v.baseVersaoId());
            ps.setString(5, v.hashArtefato());
            ps.setString(6, v.versaoCodigo());
            ps.setString(7, v.parametros());
        });
    }

    @Override
    public void atualizarStatusDaVersao(UUID versaoId, StatusVersao status) {
        executar("UPDATE ml.versao_modelo SET status = ? WHERE id = ?", ps -> {
            ps.setString(1, status.name());
            ps.setObject(2, versaoId);
        });
    }

    @Override
    public List<VersaoDoModelo> versoes() {
        return consultar("SELECT " + COLUNAS_VERSAO + " FROM ml.versao_modelo", ps -> { }, rs -> new VersaoDoModelo(
                rs.getObject("id", UUID.class), StatusVersao.valueOf(rs.getString("status")),
                rs.getObject("execucao_id", UUID.class), rs.getObject("base_versao_id", UUID.class),
                rs.getString("hash_artefato"), rs.getString("versao_codigo"), rs.getString("parametros")));
    }

    @Override
    public Optional<UUID> versaoAtiva() {
        return ponteiro("versao_id");
    }

    @Override
    public Optional<UUID> versaoAnterior() {
        return ponteiro("versao_anterior_id");
    }

    @Override
    public void finalizar(UUID execucaoId, StatusExecucao statusFinal, Instant novoWatermark, UUID versaoPromovida) {
        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement("UPDATE ml.execucao_treino SET status = ? WHERE id = ?")) {
                    ps.setString(1, statusFinal.name());
                    ps.setObject(2, execucaoId);
                    ps.executeUpdate();
                }
                if (novoWatermark != null) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE ml.checkpoint_treino SET watermark = ?, ultima_execucao_id = ? WHERE job = ?")) {
                        ps.setObject(1, tempo(novoWatermark));
                        ps.setObject(2, execucaoId);
                        ps.setString(3, JOB);
                        ps.executeUpdate();
                    }
                }
                if (versaoPromovida != null) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO ml.ponteiro_modelo_ativo (modelo, versao_id, versao_anterior_id, alterado_em)"
                                    + " VALUES (?, ?, NULL, now()) ON CONFLICT (modelo) DO UPDATE SET"
                                    + " versao_anterior_id = ml.ponteiro_modelo_ativo.versao_id,"
                                    + " versao_id = EXCLUDED.versao_id, alterado_em = now()")) {
                        ps.setString(1, MODELO);
                        ps.setObject(2, versaoPromovida);
                        ps.executeUpdate();
                    }
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("transação final do job desfeita", e);
        }
    }

    @Override
    public void trocarPonteiro(UUID novaVersao, UUID versaoAnterior) {
        executar("INSERT INTO ml.ponteiro_modelo_ativo (modelo, versao_id, versao_anterior_id, alterado_em)"
                + " VALUES (?, ?, ?, now()) ON CONFLICT (modelo) DO UPDATE SET versao_id = EXCLUDED.versao_id,"
                + " versao_anterior_id = EXCLUDED.versao_anterior_id, alterado_em = now()", ps -> {
                    ps.setString(1, MODELO);
                    ps.setObject(2, novaVersao);
                    ps.setObject(3, versaoAnterior);
                });
    }

    // ------------------------------------------------------------------

    /** Coluna NULL (ex.: sem versão anterior) → Optional.empty(). D-01: findFirst() sobre elemento null lançava NPE. */
    private Optional<UUID> ponteiro(String coluna) {
        return consultar("SELECT " + coluna + " FROM ml.ponteiro_modelo_ativo WHERE modelo = ?",
                ps -> ps.setString(1, MODELO), rs -> Optional.ofNullable(rs.getObject(coluna, UUID.class)))
                .stream().findFirst().flatMap(valor -> valor);
    }

    private static ExecucaoDeTreino execucao(ResultSet rs) throws SQLException {
        return new ExecucaoDeTreino(rs.getObject("id", UUID.class), rs.getDate("data_referencia").toLocalDate(),
                StatusExecucao.valueOf(rs.getString("status")), instante(rs, "watermark_inicio"),
                instante(rs, "corte"), rs.getString("hash_dados"));
    }

    private static Instant instante(ResultSet rs, String coluna) throws SQLException {
        OffsetDateTime t = rs.getObject(coluna, OffsetDateTime.class);
        return t == null ? null : t.toInstant();
    }

    private static OffsetDateTime tempo(Instant instante) {
        return instante == null ? null : instante.atOffset(ZoneOffset.UTC);
    }

    @FunctionalInterface
    private interface Parametros {
        void aplicar(PreparedStatement ps) throws SQLException;
    }

    @FunctionalInterface
    private interface Linha<T> {
        T ler(ResultSet rs) throws SQLException;
    }

    private void executar(String sql, Parametros parametros) {
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            parametros.aplicar(ps);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("falha no registry de modelos", e);
        }
    }

    private <T> List<T> consultar(String sql, Parametros parametros, Linha<T> linha) {
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            parametros.aplicar(ps);
            List<T> resultado = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(linha.ler(rs));
                }
            }
            return resultado;
        } catch (SQLException e) {
            throw new IllegalStateException("falha no registry de modelos", e);
        }
    }
}
