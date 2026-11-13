package br.senac.footfanatics.treino;

import java.util.Optional;
import java.util.UUID;

/**
 * Carrega a versão ativa (somente leitura). Só troca de versão em memória depois de uma carga válida;
 * sem modelo ativo, fallback sem modelo (ADR-013; §6.3 item 14).
 */
public class LeitorDoModeloAtivo {

    private final RegistryDeModelos registry;
    private final ArmazenamentoDeArtefatos armazenamento;
    private final CarregadorDeModelo carregador;
    private volatile ModeloCarregado emUso;

    public LeitorDoModeloAtivo(RegistryDeModelos registry, ArmazenamentoDeArtefatos armazenamento,
                               CarregadorDeModelo carregador) {
        this.registry = registry;
        this.armazenamento = armazenamento;
        this.carregador = carregador;
    }

    public void recarregar() {
        Optional<UUID> ativa = registry.versaoAtiva();
        if (ativa.isEmpty()) {
            emUso = null;
            return;
        }
        if (emUso != null && emUso.versaoId().equals(ativa.get())) {
            return;
        }
        Optional<VersaoDoModelo> versao = registry.versoes().stream()
                .filter(v -> v.id().equals(ativa.get())).findFirst();
        Optional<byte[]> artefato = versao.flatMap(v -> armazenamento.ler(v.hashArtefato()));
        if (artefato.isEmpty()) {
            return; // mantém a versão atual
        }
        try {
            emUso = carregador.carregar(ativa.get(), artefato.get());
        } catch (RuntimeException cargaInvalida) {
            // mantém a versão atual: nunca serve um modelo meio carregado
        }
    }

    public Optional<ModeloCarregado> modeloEmUso() {
        return Optional.ofNullable(emUso);
    }
}
