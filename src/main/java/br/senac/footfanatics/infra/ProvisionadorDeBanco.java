package br.senac.footfanatics.infra;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

/**
 * Cria (de forma idempotente) os schemas app, eventos e ml, as tabelas do modelo lógico de §3
 * e os papéis api_app, batch_leitor e batch_registry com os privilégios de §3 (ADR-015, ADR-010).
 * Nunca apaga dados: pode ser executado várias vezes sobre o mesmo banco.
 */
public class ProvisionadorDeBanco {

    private static final String[] ESQUEMA = {
        "CREATE SCHEMA IF NOT EXISTS app",
        "CREATE SCHEMA IF NOT EXISTS eventos",
        "CREATE SCHEMA IF NOT EXISTS ml",

        "CREATE TABLE IF NOT EXISTS app.usuario (id uuid PRIMARY KEY, email text NOT NULL, hash_senha text NOT NULL, status text)",
        "CREATE TABLE IF NOT EXISTS app.sessao (id uuid PRIMARY KEY, usuario_id uuid NOT NULL REFERENCES app.usuario(id),"
                + " dispositivo text, hash_token_de_acesso text, acesso_expira_em timestamptz, hash_refresh_token text,"
                + " refresh_expira_em timestamptz, hash_refresh_anterior text, renovada_em timestamptz,"
                + " versao int NOT NULL DEFAULT 0, revogada_em timestamptz)",
        "CREATE TABLE IF NOT EXISTS app.plano (codigo text PRIMARY KEY)",
        "INSERT INTO app.plano (codigo) VALUES ('FREE'), ('PREMIUM') ON CONFLICT DO NOTHING",
        "CREATE TABLE IF NOT EXISTS app.assinatura (id uuid PRIMARY KEY, usuario_id uuid NOT NULL REFERENCES app.usuario(id),"
                + " plano text NOT NULL REFERENCES app.plano(codigo), inicio_vigencia timestamptz, fim_vigencia timestamptz,"
                + " cancelada_em timestamptz)",
        "CREATE TABLE IF NOT EXISTS app.clube (id uuid PRIMARY KEY, nome text NOT NULL)",
        "CREATE TABLE IF NOT EXISTS app.jogo (id uuid PRIMARY KEY, mandante_id uuid REFERENCES app.clube(id),"
                + " visitante_id uuid REFERENCES app.clube(id), data_hora timestamptz, placar text, status text)",
        "CREATE TABLE IF NOT EXISTS app.materia (id uuid PRIMARY KEY, titulo text NOT NULL, classificacao text,"
                + " publicada_em timestamptz)",

        "CREATE TABLE IF NOT EXISTS eventos.evento_de_entrada (seq bigserial PRIMARY KEY, id_evento uuid NOT NULL UNIQUE,"
                + " tipo text NOT NULL, versao_esquema int NOT NULL, sujeito_pseudonimo text, ocorrido_em timestamptz,"
                + " registrado_em timestamptz NOT NULL DEFAULT now())",

        "CREATE TABLE IF NOT EXISTS ml.execucao_treino (id uuid PRIMARY KEY, data_referencia date NOT NULL UNIQUE,"
                + " status text NOT NULL, watermark_inicio timestamptz, corte timestamptz, hash_dados text)",
        "CREATE TABLE IF NOT EXISTS ml.versao_modelo (id uuid PRIMARY KEY, status text NOT NULL,"
                + " execucao_id uuid REFERENCES ml.execucao_treino(id), base_versao_id uuid REFERENCES ml.versao_modelo(id),"
                + " hash_artefato text NOT NULL, versao_codigo text, parametros text)",
        "CREATE TABLE IF NOT EXISTS ml.checkpoint_treino (job text PRIMARY KEY, watermark timestamptz NOT NULL,"
                + " ultima_execucao_id uuid REFERENCES ml.execucao_treino(id))",
        "INSERT INTO ml.checkpoint_treino (job, watermark) VALUES ('treino-diario', 'epoch') ON CONFLICT DO NOTHING",
        "CREATE TABLE IF NOT EXISTS ml.ponteiro_modelo_ativo (modelo text PRIMARY KEY,"
                + " versao_id uuid REFERENCES ml.versao_modelo(id), versao_anterior_id uuid REFERENCES ml.versao_modelo(id),"
                + " alterado_em timestamptz)",
        "CREATE TABLE IF NOT EXISTS ml.quarentena (id bigserial PRIMARY KEY, execucao_id uuid REFERENCES ml.execucao_treino(id),"
                + " seq_evento bigint, motivo text)",

        // Versão imutável depois de registrada: só o status muda (§6.3 item 11).
        "CREATE OR REPLACE FUNCTION ml.impedir_alteracao_de_versao() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN"
                + " IF NEW.hash_artefato IS DISTINCT FROM OLD.hash_artefato OR NEW.parametros IS DISTINCT FROM OLD.parametros"
                + " OR NEW.execucao_id IS DISTINCT FROM OLD.execucao_id OR NEW.base_versao_id IS DISTINCT FROM OLD.base_versao_id"
                + " OR NEW.versao_codigo IS DISTINCT FROM OLD.versao_codigo THEN"
                + " RAISE EXCEPTION 'versao_modelo é imutável (só o status muda)'; END IF; RETURN NEW; END $$",
        "DROP TRIGGER IF EXISTS versao_modelo_imutavel ON ml.versao_modelo",
        "CREATE TRIGGER versao_modelo_imutavel BEFORE UPDATE ON ml.versao_modelo"
                + " FOR EACH ROW EXECUTE FUNCTION ml.impedir_alteracao_de_versao()",
    };

    public void provisionar(DataSource admin, CredenciaisDosPapeis credenciais) {
        try (Connection con = admin.getConnection(); Statement st = con.createStatement()) {
            for (String sql : ESQUEMA) {
                st.execute(sql);
            }
            papel(con, st, "api_app", credenciais.senhaApiApp());
            papel(con, st, "batch_leitor", credenciais.senhaBatchLeitor());
            papel(con, st, "batch_registry", credenciais.senhaBatchRegistry());
            for (String sql : privilegios()) {
                st.execute(sql);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("falha ao provisionar o banco", e);
        }
    }

    private static String[] privilegios() {
        return new String[] {
            "REVOKE ALL ON SCHEMA app, eventos, ml FROM PUBLIC",
            // api_app: lê e escreve app; só insere em eventos; só lê ponteiro e versão em ml.
            "GRANT USAGE ON SCHEMA app, eventos, ml TO api_app",
            "GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA app TO api_app",
            "GRANT INSERT ON eventos.evento_de_entrada TO api_app",
            "GRANT USAGE ON ALL SEQUENCES IN SCHEMA eventos TO api_app",
            "GRANT SELECT ON ml.ponteiro_modelo_ativo, ml.versao_modelo TO api_app",
            // batch_leitor: só lê eventos; nenhum acesso a app.
            "GRANT USAGE ON SCHEMA eventos TO batch_leitor",
            "GRANT SELECT ON eventos.evento_de_entrada TO batch_leitor",
            // batch_registry: escreve em ml.
            "GRANT USAGE ON SCHEMA ml TO batch_registry",
            "GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA ml TO batch_registry",
            "GRANT USAGE ON ALL SEQUENCES IN SCHEMA ml TO batch_registry",
        };
    }

    /**
     * Cria ou atualiza o papel com a senha da configuração. CREATE/ALTER ROLE não aceitam parâmetro de
     * PreparedStatement, então o próprio PostgreSQL monta o comando com format(%I, %L): o nome sai como
     * identificador e a senha como literal com quoting do servidor. Antes, a senha era concatenada num bloco
     * DO $$ ... $$, e uma senha contendo "$$" encerrava o bloco e injetava SQL (SonarCloud java:S2077).
     */
    private static void papel(Connection con, Statement st, String nome, String senha) throws SQLException {
        boolean existe;
        try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM pg_roles WHERE rolname = ?")) {
            ps.setString(1, nome);
            try (ResultSet rs = ps.executeQuery()) {
                existe = rs.next();
            }
        }
        String modelo = existe
                ? "SELECT format('ALTER ROLE %I LOGIN PASSWORD %L', ?, ?)"
                : "SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', ?, ?)";
        String comando;
        try (PreparedStatement ps = con.prepareStatement(modelo)) {
            ps.setString(1, nome);
            ps.setString(2, senha);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                comando = rs.getString(1);
            }
        }
        st.execute(comando);
    }
}
