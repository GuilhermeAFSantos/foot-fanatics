package br.senac.footfanatics.conteudo;

import br.senac.footfanatics.acesso.PoliticaDeAcesso;
import br.senac.footfanatics.acesso.Principal;
import br.senac.footfanatics.erro.FalhaDeAcesso;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/** Conteúdo (E4); consulta a PoliticaDeAcesso antes de devolver qualquer item (ADR-002). */
public class ServicoDeConteudo {

    private final RepositorioDeConteudo repositorio;
    private final PoliticaDeAcesso politica;

    public ServicoDeConteudo(RepositorioDeConteudo repositorio, PoliticaDeAcesso politica) {
        this.repositorio = repositorio;
        this.politica = politica;
    }

    public Materia buscarMateria(Principal principal, UUID id) {
        Materia materia = repositorio.buscarMateria(id)
                .orElseThrow(() -> new NoSuchElementException("matéria não encontrada"));
        politica.exigirAcessoAoConteudo(principal, materia.classificacao());
        return materia;
    }

    /**
     * Só devolve os itens que a política libera. Omitir os negados é provisório:
     * QT-12 (omitir × listar sem corpo) segue em aberto.
     */
    public List<Materia> listarMaterias(Principal principal) {
        return repositorio.listarMaterias().stream()
                .filter(m -> permitido(principal, m))
                .toList();
    }

    private boolean permitido(Principal principal, Materia materia) {
        try {
            politica.exigirAcessoAoConteudo(principal, materia.classificacao());
            return true;
        } catch (FalhaDeAcesso negado) {
            return false;
        }
    }
}
