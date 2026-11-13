package br.senac.footfanatics.apoio;

import br.senac.footfanatics.identidade.CanalDeRecuperacao;
import java.util.ArrayList;
import java.util.List;

/** Captura o que seria enviado ao titular. */
public class CanalDeRecuperacaoFalso implements CanalDeRecuperacao {

    public record Envio(String email, String codigo) {
    }

    public final List<Envio> enviados = new ArrayList<>();

    @Override
    public void enviar(String email, String codigo) {
        enviados.add(new Envio(email, codigo));
    }

    public String ultimoCodigoPara(String email) {
        for (int i = enviados.size() - 1; i >= 0; i--) {
            if (enviados.get(i).email().equals(email)) {
                return enviados.get(i).codigo();
            }
        }
        throw new AssertionError("nenhum código enviado para " + email);
    }
}
