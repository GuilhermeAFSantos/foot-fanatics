package br.senac.footfanatics.treino;

import static br.senac.footfanatics.apoio.ValoresDeTeste.T0;
import static br.senac.footfanatics.apoio.ValoresDeTeste.VERSAO_ESQUEMA_ATUAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.senac.footfanatics.apoio.CenarioDoJob;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RS-07: esquema N e N−1, quarentena e limite (docs/arquitetura.md:799; ADR-007). Limite de teste: 10%. */
class ValidadorDeDadosTest {

    private CenarioDoJob c;

    @BeforeEach
    void montar() {
        c = new CenarioDoJob();
    }

    private List<EventoDeEntrada> lote(int validos, int invalidos) {
        List<EventoDeEntrada> l = new ArrayList<>();
        for (int i = 0; i < validos; i++) {
            l.add(c.evento(T0, VERSAO_ESQUEMA_ATUAL));
        }
        for (int i = 0; i < invalidos; i++) {
            l.add(c.evento(T0, 999));
        }
        return l;
    }

    @Test
    @DisplayName("RS-07 · CT-S07.01 · esquemas N e N−1 são aceitos")
    void rs07_cts07_01_esquemasNeNMenos1() {
        List<EventoDeEntrada> l = List.of(c.evento(T0, VERSAO_ESQUEMA_ATUAL), c.evento(T0, VERSAO_ESQUEMA_ATUAL - 1));

        ResultadoDaValidacao r = c.validador.validar(l);

        assertTrue(r.aprovado());
        assertEquals(2, r.validos().size());
        assertTrue(r.emQuarentena().isEmpty());
    }

    @Test
    @DisplayName("RS-07 · CT-S07.02 · esquema N+1 ou desconhecido vai para a quarentena")
    void rs07_cts07_02_esquemaDesconhecidoEmQuarentena() {
        EventoDeEntrada futuro = c.evento(T0, VERSAO_ESQUEMA_ATUAL + 1);
        EventoDeEntrada antigo = c.evento(T0, VERSAO_ESQUEMA_ATUAL - 2);
        List<EventoDeEntrada> l = new ArrayList<>(lote(50, 0));
        l.add(futuro);
        l.add(antigo);

        ResultadoDaValidacao r = c.validador.validar(l);

        assertTrue(r.emQuarentena().contains(futuro));
        assertTrue(r.emQuarentena().contains(antigo));
        assertFalse(r.validos().contains(futuro) || r.validos().contains(antigo));
    }

    @Test
    @DisplayName("RS-07 · CT-S07.03a · taxa de quarentena abaixo do limite (9%) → aprovado")
    void rs07_cts07_03a_abaixoDoLimite() {
        ResultadoDaValidacao r = c.validador.validar(lote(91, 9));

        assertTrue(r.aprovado());
    }

    @Test
    @DisplayName("RS-07 · CT-S07.03b · taxa de quarentena acima do limite (11%) → reprovado")
    void rs07_cts07_03b_acimaDoLimite() {
        ResultadoDaValidacao r = c.validador.validar(lote(89, 11));

        assertFalse(r.aprovado());
    }

    @Test
    @Disabled("Bloqueado: AM-02, taxa exatamente igual ao limite reprova?")
    @DisplayName("RS-07 · CT-S07.03c · taxa igual ao limite")
    void rs07_cts07_03c_igualAoLimite() {
    }

    @Test
    @Disabled("Bloqueado: volume mínimo da janela não definido (§6.3 item 8)")
    @DisplayName("RS-07 · CT-S07.05 · janela abaixo do volume mínimo")
    void rs07_cts07_05_volumeMinimo() {
    }
}
