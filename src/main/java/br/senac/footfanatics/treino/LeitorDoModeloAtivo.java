package br.senac.footfanatics.treino;

import java.util.Optional;

/** Carrega a versão ativa com fallback sem modelo (ADR-013). */
public class LeitorDoModeloAtivo {

    public LeitorDoModeloAtivo(RegistryDeModelos registry, ArmazenamentoDeArtefatos armazenamento,
                               CarregadorDeModelo carregador) {
    }

    public void recarregar() {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    public Optional<ModeloCarregado> modeloEmUso() {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }
}
