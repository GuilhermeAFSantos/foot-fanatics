package br.senac.footfanatics.identidade;

/** Canal externo de recuperação (tecnologia pendente, L-05). */
public interface CanalDeRecuperacao {
    void enviar(String email, String codigo);
}
