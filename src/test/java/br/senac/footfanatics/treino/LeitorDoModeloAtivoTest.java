package br.senac.footfanatics.treino;

import static br.senac.footfanatics.apoio.CenarioDoJob.W0;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.senac.footfanatics.apoio.ArmazenamentoEmMemoria;
import br.senac.footfanatics.apoio.CarregadorFalso;
import br.senac.footfanatics.apoio.RegistryEmMemoria;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RS-13: o leitor só troca após carga válida; senão mantém a atual ou faz fallback (docs/arquitetura.md:805; ADR-013). */
class LeitorDoModeloAtivoTest {

    private RegistryEmMemoria registry;
    private ArmazenamentoEmMemoria armazenamento;
    private LeitorDoModeloAtivo leitor;

    @BeforeEach
    void montar() {
        registry = new RegistryEmMemoria(W0);
        armazenamento = new ArmazenamentoEmMemoria();
        leitor = new LeitorDoModeloAtivo(registry, armazenamento, new CarregadorFalso());
    }

    private UUID versaoCom(String conteudo, StatusVersao status) {
        String hash = conteudo == null ? "inexistente" : armazenamento.gravar(conteudo.getBytes(StandardCharsets.UTF_8));
        UUID id = UUID.randomUUID();
        registry.semear(new VersaoDoModelo(id, status, UUID.randomUUID(), null, hash, "codigo", "p"));
        return id;
    }

    private UUID ativaCarregada() {
        UUID v1 = versaoCom("modelo-1", StatusVersao.ATIVO);
        registry.semearPonteiro(v1, null);
        leitor.recarregar();
        return v1;
    }

    @Test
    @DisplayName("RS-13 · CT-S13.01 · ponteiro muda para versão válida → API passa a usá-la após recarga")
    void rs13_cts13_01_trocaAposCargaValida() {
        UUID v1 = ativaCarregada();
        UUID v2 = versaoCom("modelo-2", StatusVersao.ATIVO);
        registry.semearPonteiro(v2, v1);

        leitor.recarregar();

        assertEquals(Optional.of(new ModeloCarregado(v2)), leitor.modeloEmUso());
    }

    @Test
    @DisplayName("RS-13 · CT-S13.02 · artefato corrompido → mantém a versão atual")
    void rs13_cts13_02_corrompidoMantemAtual() {
        UUID v1 = ativaCarregada();
        UUID v2 = versaoCom("corrompido", StatusVersao.ATIVO);
        registry.semearPonteiro(v2, v1);

        leitor.recarregar();

        assertEquals(Optional.of(new ModeloCarregado(v1)), leitor.modeloEmUso());
    }

    @Test
    @DisplayName("RS-13 · CT-S13.03 · sem modelo ativo → fallback sem modelo, sem erro")
    void rs13_cts13_03_fallbackSemModelo() {
        assertDoesNotThrow(() -> leitor.recarregar());

        assertTrue(leitor.modeloEmUso().isEmpty());
    }

    @Test
    @DisplayName("RS-13 · CT-S13.04 · artefato ausente do armazenamento → nunca serve modelo meio carregado")
    void rs13_cts13_04_artefatoAusenteMantemAtual() {
        UUID v1 = ativaCarregada();
        UUID v2 = versaoCom(null, StatusVersao.ATIVO);
        registry.semearPonteiro(v2, v1);

        leitor.recarregar();

        assertEquals(Optional.of(new ModeloCarregado(v1)), leitor.modeloEmUso());
    }
}
