package br.senac.footfanatics.apoio;

import static br.senac.footfanatics.apoio.ValoresDeTeste.T0;
import static br.senac.footfanatics.apoio.ValoresDeTeste.TOLERANCIA_REUSO;
import static br.senac.footfanatics.apoio.ValoresDeTeste.TTL_ACESSO;
import static br.senac.footfanatics.apoio.ValoresDeTeste.TTL_REFRESH;

import br.senac.footfanatics.acesso.Classificacao;
import br.senac.footfanatics.acesso.PoliticaDeAcesso;
import br.senac.footfanatics.acesso.Principal;
import br.senac.footfanatics.assinatura.Assinatura;
import br.senac.footfanatics.assinatura.Plano;
import br.senac.footfanatics.assinatura.ServicoDeAssinatura;
import br.senac.footfanatics.conteudo.Materia;
import br.senac.footfanatics.conteudo.ServicoDeConteudo;
import br.senac.footfanatics.sessao.ConfiguracaoDeSessao;
import br.senac.footfanatics.sessao.ServicoDeSessao;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Monta sessão, assinatura, política e conteúdo com dublês em memória. Só Arrange, sem regra. */
public class CenarioDeAcesso {
    public final RelogioAjustavel relogio = new RelogioAjustavel(T0);
    public final AutenticadorFalso autenticador = new AutenticadorFalso();
    public final SessoesEmMemoria sessoes = new SessoesEmMemoria();
    public final AssinaturasEmMemoria assinaturas = new AssinaturasEmMemoria();
    public final ConteudoEmMemoria conteudo = new ConteudoEmMemoria();

    public final ServicoDeSessao sessao = new ServicoDeSessao(autenticador, sessoes, relogio,
            new ConfiguracaoDeSessao(TTL_ACESSO, TTL_REFRESH, TOLERANCIA_REUSO));
    public final ServicoDeAssinatura assinatura = new ServicoDeAssinatura(assinaturas, relogio);
    public final PoliticaDeAcesso politica = new PoliticaDeAcesso(assinatura);
    public final ServicoDeConteudo servicoDeConteudo = new ServicoDeConteudo(conteudo, politica);

    public UUID usuarioFree() {
        UUID id = UUID.randomUUID();
        assinaturas.salvar(new Assinatura(UUID.randomUUID(), id, Plano.FREE, T0.minus(Duration.ofDays(30)), null, null));
        return id;
    }

    public UUID usuarioPremium(Instant inicio, Instant fim) {
        UUID id = UUID.randomUUID();
        assinaturas.salvar(new Assinatura(UUID.randomUUID(), id, Plano.PREMIUM, inicio, fim, null));
        return id;
    }

    /** Premium vigente em T0 por 30 dias. */
    public UUID usuarioPremiumVigente() {
        return usuarioPremium(T0.minus(Duration.ofDays(1)), T0.plus(Duration.ofDays(30)));
    }

    /** Premium cuja vigência terminou antes de T0. */
    public UUID usuarioPremiumVencido() {
        return usuarioPremium(T0.minus(Duration.ofDays(60)), T0.minus(Duration.ofDays(30)));
    }

    public Materia materia(Classificacao classificacao) {
        Materia m = new Materia(UUID.randomUUID(), "Título " + classificacao, "Corpo " + classificacao, classificacao);
        conteudo.salvar(m);
        return m;
    }

    public static Principal anonimo() {
        return Principal.anonimo();
    }

    public static Principal de(UUID usuario) {
        return Principal.de(usuario);
    }
}
