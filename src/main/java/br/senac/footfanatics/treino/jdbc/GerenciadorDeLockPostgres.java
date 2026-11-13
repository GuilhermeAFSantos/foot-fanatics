package br.senac.footfanatics.treino.jdbc;

import br.senac.footfanatics.treino.GerenciadorDeLock;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 * Advisory lock do PostgreSQL em conexão dedicada (ADR-009). Se o processo cair, a conexão
 * termina e o PostgreSQL libera o lock.
 */
public class GerenciadorDeLockPostgres implements GerenciadorDeLock {

    /** Chave fixa do Job de Treino Diário. */
    static final long CHAVE_DO_JOB = 0x466F6F7446616EL; // "FootFan"

    private final DataSource dataSource;
    private Connection conexao;

    public GerenciadorDeLockPostgres(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public synchronized boolean tentarAdquirir() {
        if (conexao != null) {
            return true;
        }
        try {
            Connection con = dataSource.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
                ps.setLong(1, CHAVE_DO_JOB);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getBoolean(1)) {
                        conexao = con;
                        return true;
                    }
                }
            }
            con.close();
            return false;
        } catch (SQLException e) {
            throw new IllegalStateException("falha ao tentar o lock do job", e);
        }
    }

    @Override
    public synchronized void liberar() {
        if (conexao == null) {
            return;
        }
        try (Connection con = conexao; PreparedStatement ps = con.prepareStatement("SELECT pg_advisory_unlock(?)")) {
            ps.setLong(1, CHAVE_DO_JOB);
            ps.execute();
        } catch (SQLException e) {
            // conexão perdida: o PostgreSQL já liberou o lock
        } finally {
            conexao = null;
        }
    }
}
