package br.senac.footfanatics.infra;

import javax.sql.DataSource;

/** Cria os schemas app, eventos e ml e os papéis api_app, batch_leitor e batch_registry (§3; ADR-015). */
public class ProvisionadorDeBanco {

    public void provisionar(DataSource admin, CredenciaisDosPapeis credenciais) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }
}
