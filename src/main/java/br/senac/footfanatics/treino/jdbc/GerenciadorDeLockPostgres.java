package br.senac.footfanatics.treino.jdbc;

import br.senac.footfanatics.treino.GerenciadorDeLock;
import javax.sql.DataSource;

/** Advisory lock do PostgreSQL (ADR-009). */
public class GerenciadorDeLockPostgres implements GerenciadorDeLock {

    public GerenciadorDeLockPostgres(DataSource dataSource) {
    }

    @Override
    public boolean tentarAdquirir() {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    @Override
    public void liberar() {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }
}
