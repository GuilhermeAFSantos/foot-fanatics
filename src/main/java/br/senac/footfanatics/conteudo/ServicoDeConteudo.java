package br.senac.footfanatics.conteudo;

import br.senac.footfanatics.acesso.PoliticaDeAcesso;
import br.senac.footfanatics.acesso.Principal;
import java.util.List;
import java.util.UUID;

/** Conteúdo (E4); consulta a PoliticaDeAcesso antes de devolver qualquer item (ADR-002). */
public class ServicoDeConteudo {

    public ServicoDeConteudo(RepositorioDeConteudo repositorio, PoliticaDeAcesso politica) {
    }

    public Materia buscarMateria(Principal principal, UUID id) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }

    public List<Materia> listarMaterias(Principal principal) {
        throw new UnsupportedOperationException("não implementado (fase red)");
    }
}
