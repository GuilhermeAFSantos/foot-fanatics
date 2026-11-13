package br.senac.footfanatics.treino;

import static br.senac.footfanatics.apoio.ValoresDeTeste.PISO;
import static br.senac.footfanatics.apoio.ValoresDeTeste.TOLERANCIA_GATE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.senac.footfanatics.apoio.CenarioDoJob;
import java.util.OptionalDouble;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * RS-08: o gate aprova se dados e sanidade passam e métrica do candidato ≥ métrica do ativo − tolerância;
 * sem ativo, sanidade + piso (docs/arquitetura.md:803). A métrica real é pendente (Q-09); o teste usa
 * uma pontuação abstrata em que maior é melhor, como a própria regra pressupõe ("≥").
 */
class GateDePromocaoTest {

    private static final double ATIVO = 0.75;
    private static final double EPS = 0.001;

    private final GateDePromocao gate = new CenarioDoJob().gate;

    @Test
    @DisplayName("RS-08 · CT-S08.01 · candidato melhor que o ativo, dados e sanidade ok → aprova")
    void rs08_cts08_01_candidatoMelhorAprova() {
        assertTrue(gate.aprova(true, true, 0.90, OptionalDouble.of(ATIVO)));
    }

    @Test
    @DisplayName("RS-08 · CT-S08.02 · candidato = ativo − tolerância → aprova (≥)")
    void rs08_cts08_02_exatamenteNaToleranciaAprova() {
        assertTrue(gate.aprova(true, true, ATIVO - TOLERANCIA_GATE, OptionalDouble.of(ATIVO)));
    }

    @Test
    @DisplayName("RS-08 · CT-S08.03 · candidato = ativo − tolerância − ε → reprova")
    void rs08_cts08_03_abaixoDaToleranciaReprova() {
        assertFalse(gate.aprova(true, true, ATIVO - TOLERANCIA_GATE - EPS, OptionalDouble.of(ATIVO)));
    }

    @Test
    @DisplayName("RS-08 · CT-S08.04 · sanidade ou dados reprovados → reprova mesmo com métrica melhor")
    void rs08_cts08_04_sanidadeEDadosSaoObrigatorios() {
        assertFalse(gate.aprova(true, false, 0.99, OptionalDouble.of(ATIVO)));
        assertFalse(gate.aprova(false, true, 0.99, OptionalDouble.of(ATIVO)));
    }

    @Test
    @DisplayName("RS-08 · CT-S08.05a · primeira execução: sanidade ok e métrica = piso → aprova")
    void rs08_cts08_05a_primeiraExecucaoNoPiso() {
        assertTrue(gate.aprova(true, true, PISO, OptionalDouble.empty()));
    }

    @Test
    @DisplayName("RS-08 · CT-S08.05b · primeira execução: métrica = piso − ε → reprova")
    void rs08_cts08_05b_primeiraExecucaoAbaixoDoPiso() {
        assertFalse(gate.aprova(true, true, PISO - EPS, OptionalDouble.empty()));
    }
}
