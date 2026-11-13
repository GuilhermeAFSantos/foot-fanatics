# Foot Fanatics — Avaliação arquitetural ATAM, ciclo 01

> **Método:** ATAM (Bass, Clements e Kazman), fase 1 (passos 1 a 6), conduzida por um avaliador sobre artefatos. A fase 2 (brainstorm e votação com stakeholders, passos 7 e 8) **não foi feita**. Por isso toda prioridade aqui é **proposta do avaliador, com validação pendente**.
> **Data:** 2026-11-13. **Base avaliada:** commit `0347ff9` da `main`.
> **Diferença em relação à Parte C de `docs/arquitetura.md`:** aquela avaliação foi feita sobre o **desenho**. Este ciclo confronta o desenho com o **código e os runs de CI** e cita arquivo e linha como evidência.
> **Regras seguidas:** nenhum código alterado; nenhum requisito novo; o que é hipótese está marcado como tal.

**Prefixos deste documento:**

| Prefixo | Significado |
|---------|-------------|
| DN | Direcionador de negócio |
| AC | Cenário de atributo de qualidade |
| SE | Ponto de sensibilidade |
| TO | Ponto de trade-off |
| RC | Risco |
| NR | Não risco |
| TR | Tema de risco |
| DG | Divergência entre documento e código |
| AP | Ação priorizada |

Os IDs da arquitetura (RAS, RS, DA, ADR, CEN, Q) foram preservados. Caminhos de código são relativos a `src/main/java/br/senac/footfanatics/`.

---

## 0. Fontes consultadas

| Fonte | O que foi lido |
|-------|----------------|
| `docs/prd.md` | Personas P1–P3 (`:6-26`, **geradas por IA, validação pendente**, `:8`); escopos E1–E4 (`:27-31`); fora de escopo (`:32-33`) |
| `README.md` | Descrição (`:3`), escopos detalhados, seção de integração contínua |
| `docs/arquitetura.md` | Parte A (drivers), Parte B (C4, §4, §6–§11), Parte C (ATAM sobre o desenho), Parte D (ADR-001 a ADR-016, matriz D5) |
| `docs/testes/plano-tdd.md` | §7: resultado da fase green e decisões provisórias (§7.4) |
| Código de produção | Os 63 arquivos `.java` de `src/main` e o `application.properties` |
| Testes | 12 classes em `src/test`, mais os dublês de `apoio/` |
| GitHub Actions | Runs `43513343656` (push na `main`, ✅), `43512113294` (✅), `43511699150` (❌ quebra proposital), `43511428888` (✅) e `43506395202` (❌, `master` na fase red, anterior ao CI atual) |

**Fatos estruturais verificados no código**, por busca no repositório:

| Fato | Resultado da busca |
|------|--------------------|
| Anotações Spring (`@RestController`, `@Configuration`, `@Bean`, `@Component`, `@ControllerAdvice`, `@Scheduled`) em `src/main` | **0** |
| Uso de JPA (`jakarta.persistence`, `JpaRepository`) | **0**. O starter está no `pom.xml`, mas não é usado. |
| Implementações de produção de `Repositorio*` (usuários, sessões, assinaturas, conteúdo, eventos) | **0**. Só existem dublês em memória nos testes. |
| `main()` além de `FootFanaticsApplication` (ponto de entrada do Job) | **0** |
| Chamadas de produção a `OrquestradorDoJob`, `RegistradorDeEventos`, `LeitorDoModeloAtivo` e `TratadorDeErros` | **0** fora das próprias classes |
| Arquivos com logger | **0** |
| `application.properties` | Só `spring.application.name` (`:1`) |

Em resumo: o repositório tem o **núcleo de domínio e de aplicação** testado (199 testes no CI, run `43513343656`), mas **não monta um sistema executável** de ponta a ponta. Essa é a constatação central deste ciclo (TR-01).

---

## 1. Direcionadores de negócio

Visão, problema e métricas de sucesso continuam em `TODO` (`docs/prd.md:2-5,34-35`; lacuna L-01). Por isso **nenhum direcionador tem meta de negócio**. Os direcionadores abaixo vêm dos artefatos e não foram criados aqui.

| ID | Direcionador | Origem | Atributos que puxa |
|----|--------------|--------|--------------------|
| DN-01 | A assinatura é o diferencial do produto: conteúdo exclusivo para premium | `README.md:3`; E3/E4 (`README.md:11-12`) | Segurança (autorização), corretude temporal |
| DN-02 | Falha de autorização é vazamento, não desconforto | `docs/prd.md:8,25` (P3) | Segurança, privacidade |
| DN-03 | O assinante usa vários dispositivos e não quer ser deslogado nem perder acesso durante a vigência | `docs/prd.md:17-19` (P2, validação pendente) | Disponibilidade, consistência de sessão |
| DN-04 | É o projeto de uma disciplina de qualidade: a arquitetura precisa ser verificável | `README.md:3` | Testabilidade, observabilidade |
| DN-05 | Restrição do professor: job diário que treina só com dados novos, sem derrubar o online | R-03, INV-1 a INV-3 (`docs/arquitetura.md` §A5, §11) | Confiabilidade do batch, isolamento, custo |
| DN-06 | Restrições de stack e de escopo | R-01, R-02 | Delimitam as soluções |

---

## 2. Cenários de atributo de qualidade (seis partes)

Onde o cenário já existia na Parte C, o ID original aparece entre parênteses. Toda faixa numérica é **hipótese, com validação pendente**. Só os zeros de segurança e integridade são metas naturais do próprio requisito.

| ID | Fonte | Estímulo | Artefato | Ambiente | Resposta | Medida de resposta |
|----|-------|----------|----------|----------|----------|--------------------|
| **AC-01** (novo) | Aplicação Cliente de P1 | `GET` de matéria pública **por HTTP**, sem token | Sistema em execução (API Foot Fanatics) | Operação normal | 200 com a matéria, como na matriz §4.1 | 100% das células da matriz §4.1 respondidas corretamente **via HTTP** |
| **AC-02** (CEN-01) | Usuário free | Pede matéria exclusiva | PoliticaDeAcesso, ServicoDeConteudo | Operação normal | Negação `plano_insuficiente` sem nenhum campo do conteúdo | 0 respostas com conteúdo exclusivo para free ou anônimo |
| **AC-03** (CEN-02) | Assinante premium (P2) | Cancela e pede conteúdo exclusivo depois do instante de corte | ServicoDeAssinatura, PoliticaDeAcesso | Sessão ativa | Acesso negado a partir do instante de corte | Primeira requisição após o corte já negada. **O instante de corte é a pergunta Q-03.** |
| **AC-04** (CEN-05) | P2, ou quem tenha copiado o token | Usa o token de acesso logo depois do logout | ServicoDeSessao | Sessão dentro do TTL | `token_invalido` | Atraso entre logout e recusa = 0 s (ADR-004) |
| **AC-05** (CEN-04) | Duas abas de P2 | Renovam o mesmo refresh ao mesmo tempo | ServicoDeSessao, RepositorioDeSessoes | Operação normal | As duas recebem o mesmo par; P2 continua logada | 0 sessões inconsistentes; 0 logouts involuntários na corrida |
| **AC-06** (novo) | Operação | Reinício ou novo deploy da API | ServicoDeSessao | Sessões ativas | As sessões continuam válidas | Sessões invalidadas por reinício = 0. Hipótese: o requisito existe, mas não está escrito. |
| **AC-07** (CEN-20) | Job de Treino Diário | Lê uma janela grande durante o atendimento online | Banco Operacional, API | "Final do dia", tráfego desconhecido (Q-14) | A API mantém a latência | p95 com batch ≤ p95 sem batch × 1,15–1,25. **Hipótese** (Q-08). |
| **AC-08** (CEN-10) | Transação lenta da API | Faz commit depois do corte com `registrado_em` anterior a ele | ExtratorIncremental, CHECKPOINT | Execução em andamento | O evento entra numa janela; nunca é pulado | Eventos perdidos = 0 |
| **AC-09** (CEN-14) | Treinador | Produz um candidato pior que o ativo | GateDePromocao, OrquestradorDoJob | Execução normal | REPROVADO; ponteiro e checkpoint mantidos | Promoções abaixo de ativo − tolerância = 0 |
| **AC-10** (CEN-15) | Infraestrutura | Queda no meio da promoção | RegistryDeModelosJdbc, OrquestradorDoJob | Gate aprovou | Tudo ou nada: ponteiro, checkpoint, execução **e status das versões** | Estados divergentes = 0 em injeção de falha |
| **AC-11** (CEN-09) | Agendador ou operador | Dois disparos simultâneos | GerenciadorDeLockPostgres | Execução em andamento | Só um prossegue | Execuções simultâneas ≤ 1 |
| **AC-12** (CEN-19) | Crescimento do uso | Volume de eventos cresce várias vezes | ExtratorIncremental, RegistradorDeEventos | Execução diária | Conclui dentro da janela | Tempo proporcional à janela, não ao histórico. Janela: Q-10. |
| **AC-13** (CEN-18) | Credencial vazada do `batch_leitor` | Tenta ler `app.usuario` | Papéis do banco | Operação normal | Permissão negada | 100% negadas (SQLSTATE 42501) |
| **AC-14** (novo) | Desenvolvedor | Introduz uma regressão numa regra de negócio | Suíte + CI | Pull request | O check falha e bloqueia o merge na `main` | Regressão detectada antes do merge em 100% dos casos cobertos por teste |
| **AC-15** (novo) | Operação ou auditoria | Pede quem promoveu ou fez rollback de uma versão, e quem fez login | Auditoria (§7) | Após o fato | Registro com ator, instante e correlação | 100% das ações sensíveis rastreáveis |
| **AC-16** (CEN-12) | Deploy da API | Eventos chegam com `versao_esquema` nova | ValidadorDeDados | Execução diária | Esquema desconhecido vai para a quarentena; nunca é treinado | Treinos com esquema desconhecido = 0 |

---

## 3. Utility tree

Escala: **A** (alta), **M** (média), **B** (baixa), no formato **(importância, dificuldade)**. A dificuldade considera o **estado atual do código**, não só o desenho.

```mermaid
flowchart LR
  U["Utilidade"]
  U --> SEG["Segurança"]
  U --> CT["Corretude temporal"]
  U --> DISP["Disponibilidade"]
  U --> CB["Confiabilidade do batch"]
  U --> TST["Testabilidade"]
  U --> OBS["Observabilidade e auditoria"]
  U --> CUS["Desempenho e custo"]
  SEG --> SEG1["Autorização ponta a ponta"] --> AC01["AC-01 (A,A)"]
  SEG1 --> AC02["AC-02 (A,B)"]
  SEG --> SEG2["Revogação"] --> AC04["AC-04 (A,M)"]
  SEG --> SEG3["Isolamento de dados"] --> AC13["AC-13 (A,B)"]
  CT --> CT1["Vigência"] --> AC03["AC-03 (A,A)"]
  DISP --> DISP1["Sessão estável"] --> AC05["AC-05 (M,B)"]
  DISP1 --> AC06["AC-06 (M,M)"]
  DISP --> DISP2["Isolamento online e batch"] --> AC07["AC-07 (A,A)"]
  CB --> CB1["Dados novos"] --> AC08["AC-08 (A,M)"]
  CB1 --> AC16["AC-16 (M,B)"]
  CB --> CB2["Promoção segura"] --> AC09["AC-09 (A,M)"]
  CB2 --> AC10["AC-10 (A,M)"]
  CB --> CB3["Concorrência"] --> AC11["AC-11 (A,B)"]
  TST --> TST1["Regressão detectada"] --> AC14["AC-14 (A,B)"]
  OBS --> OBS1["Rastreabilidade"] --> AC15["AC-15 (M,A)"]
  CUS --> CUS1["Janela do batch"] --> AC12["AC-12 (M,A)"]
```

| Cenário | (I,D) | Por que esta importância | Por que esta dificuldade (no código atual) |
|---------|-------|--------------------------|--------------------------------------------|
| AC-01 | **(A,A)** | Sem isso, nenhuma regra de DN-01 ou DN-02 alcança um usuário real | Não existe camada HTTP nem persistência (DG-01, DG-02); é construir o sistema em execução |
| AC-03 | **(A,A)** | DN-01, DN-02: acesso após cancelar ou perda antes do fim | A regra não está definida (Q-03), e o código hoje **não corta** (DG-07) |
| AC-07 | **(A,A)** | INV-1 é obrigatória (DN-05) | Não há processo separado nem limites de recurso (DG-03, DG-05) |
| AC-04 | (A,M) | DN-02 | Implementado no serviço; falta o sistema em execução e a chave persistente |
| AC-08 | (A,M) | DN-05 | Mecanismo implementado; Δ e o timeout de transação da API não existem |
| AC-09 | (A,M) | DN-05 | Gate implementado; a métrica real depende de Q-09 |
| AC-10 | (A,M) | DN-05, INV-2/3 | Transação parcial (DG-06) |
| AC-02 | (A,B) | DN-01, DN-02 | Implementado e coberto pela matriz completa |
| AC-11 | (A,B) | DN-05 | Implementado e testado no PostgreSQL real |
| AC-13 | (A,B) | DN-02 | Papéis implementados e testados |
| AC-14 | (A,B) | DN-04 | CI e proteção da `main` funcionando (run vermelho comprovado) |
| AC-12 | (M,A) | DN-05; sem volume (L-08) | Leitura do histórico inteiro na memória (DG-09) |
| AC-15 | (M,A) | DN-04; DIV-03 | Nada implementado (DG-08) |
| AC-06 | (M,M) | DN-03 | Chave HMAC gerada por instância (SE-04) |
| AC-05 | (M,B) | DN-03 | Implementado e testado com threads |
| AC-16 | (M,B) | DN-05 | Implementado, mas a quarentena não é persistida (DG-10) |

---

## 4. Análise dos cenários de maior prioridade: abordagens, táticas e evidências

**Ordem:** primeiro os três (A,A), depois os quatro (A,M). As táticas seguem o catálogo de Bass, Clements e Kazman, *Software Architecture in Practice*, 4ª ed.

**Veredito:**
- **Atende:** a medida está garantida pelo código e por um teste.
- **Parcial:** o mecanismo existe, mas falta um elo.
- **Não atende:** o código não produz a resposta.

### 4.1 AC-01: autorização ponta a ponta via HTTP (A,A) → **Não atende**

| Abordagem prometida | Tática | Evidência no código |
|---------------------|--------|---------------------|
| FiltroDeAutenticacao valida o token e entrega o principal ao ServicoDeConteudo (`docs/arquitetura.md:275`, §4.3) | Autenticar atores | **Ausente:** nenhum filtro nem configuração de segurança em `src/main` (0 anotações Spring) |
| PoliticaDeAcesso central, nega por padrão (ADR-002) | Autorizar atores; Limitar exposição | `acesso/PoliticaDeAcesso.java:21-33` (regra da matriz), `:24-25` (nega o que não for PUBLICA ou EXCLUSIVA), `:41-42` (dados de outro usuário) |
| Todo caminho de conteúdo passa pela política | Limitar exposição | `conteudo/ServicoDeConteudo.java:24` (busca) e `:34,40` (listagem filtrada) |
| TratadorDeErros converte falhas em Problem Details (ADR-014) | Aderir a padrões | `erro/TratadorDeErros.java:16-25`, **sem ligação com HTTP**: nenhum `@ControllerAdvice` |

- **Testes que comprovam o núcleo:** `AcessoAoConteudoTest#rn19_ct19_03_matrizCompleta` (8 células), `#rn19_ct19_02_todoCaminhoPassaPelaPolitica`, `DadosDeUsuarioTest`, `ContratoDeErroTest`. Todos verdes no run `43513343656`.
- **Lacuna:** nenhum teste exercita HTTP, e nem poderia, porque não há rota.
- **Observação do setup, não reverificada neste ciclo:** a aplicação sobe com a configuração padrão do Spring Security e responde **401 a tudo**.

### 4.2 AC-03: cancelamento corta o acesso (A,A) → **Não atende** (regra indefinida)

| Abordagem prometida | Tática | Evidência no código |
|---------------------|--------|---------------------|
| Plano consultado a cada decisão, fora do token (ADR-003) | Autorizar atores (decisão no instante) | `acesso/PoliticaDeAcesso.java:30` → `assinatura/ServicoDeAssinatura.java:25` (`relogio.agora()`) ✅ |
| Fonte única de tempo (ADR-005) | Interfaces especializadas; Limitar não determinismo | `tempo/Relogio.java`; uso em `ServicoDeAssinatura.java:25,44` ✅ |
| Cancelamento corta a partir de `t_corte` (`docs/arquitetura.md:622`) | Revogar acesso | `ServicoDeAssinatura.java:44-48` **só grava** `canceladaEm`. `vigenteEm` (`:57-58`) **ignora** `canceladaEm`, então o acesso continua até `fimVigencia` (DG-07). |

- **Testes:** `AssinaturaTest#rn14_ct14_01` (o token não carrega privilégio) ✅. O caso central CT-13.01/02 está **desabilitado** por causa de Q-03.

### 4.3 AC-07: o batch não degrada o online (A,A) → **Não atende** (no código atual)

| Abordagem prometida | Tática | Evidência no código |
|---------------------|--------|---------------------|
| Job em processo separado (DA-06, `docs/arquitetura.md:871`; ADR-006 `:1619`) | Separar entidades; Agendar recursos | **Ausente:** não há ponto de entrada do Job nem agendador (0 `main()` além da app). Hoje o orquestrador só roda dentro dos testes. |
| `batch_leitor` com limite de conexões e `statement_timeout` (DA-13, `:878`) | Limitar tamanho de filas; Limitar tempo de execução | **Parcial:** `infra/ProvisionadorDeBanco.java:83-92` cria os papéis e privilégios, mas **sem** `CONNECTION LIMIT` e **sem** `statement_timeout` (DG-05) |
| Batch lê só a janela de `eventos` (DA-07) | Gerenciar requisições de trabalho | `treino/ExtratorIncremental.java:20-22` filtra **em memória** depois de `lerTodos()`, ou seja, depende do histórico inteiro (DG-09) |
| API isolada do resultado do batch (DA-16) | Degradação graciosa | `treino/LeitorDoModeloAtivo.java:26-41` mantém a versão atual ou faz fallback ✅, mas **nenhum código** chama `recarregar()` periodicamente (DG-04) |

- **Testes:** `LeitorDoModeloAtivoTest` ✅ (isolamento do modelo). Nenhum teste de carga: o EXP-01 da arquitetura nunca foi executado.

### 4.4 AC-04: logout revoga na hora (A,M) → **Atende no serviço; Parcial no sistema**

| Abordagem | Tática | Evidência |
|-----------|--------|-----------|
| Token opaco validado contra a SESSAO a cada requisição (ADR-004) | Revogar acesso | `sessao/ServicoDeSessao.java:125` (sessão revogada ou hash divergente → inválido), `:110` (logout revoga) |
| Revogação com controle otimista, sem se perder numa corrida | Transações | `ServicoDeSessao.java:148-149` (incrementa a versão + `substituirSeVersao`) |
| Assinatura HMAC dos tokens | Verificar integridade da mensagem | `ServicoDeSessao.java:160`; chave em `:30`, **gerada por instância** (SE-04, RC-05) |

- **Testes:** `SessaoTest#rn09_ct09_01`, `#rn06_ct06_07`, `#rn09_ct09_05` ✅.
- **Lacuna:** não há adaptador de sessão persistente (DG-02), então em produção as sessões não sobrevivem a nada.

### 4.5 AC-08: evento com commit depois do corte (A,M) → **Parcial**

| Abordagem | Tática | Evidência |
|-----------|--------|-----------|
| Janela (watermark, corte] por tempo de ingestão (ADR-008) | Carimbo de tempo | `treino/ExtratorIncremental.java:21` |
| corte = início − Δ | Limitar tempo de execução | `treino/OrquestradorDoJob.java:99` |
| Δ maior que o timeout máximo de transação da API | — | **Ausente:** não há transação de API, nem timeout configurado, nem valor de Δ (Q-15). A garantia depende de um parâmetro que não existe. |

- **Testes:** `JanelaIncrementalTest` (bordas ±1 ms) e `OrquestradorDoJobTest#rs01_cts01_08` ✅. Nenhum teste reproduz o commit tardio real: o EXP-02 nunca foi executado.

### 4.6 AC-09: candidato pior que o ativo (A,M) → **Atende** (com métrica abstrata)

| Abordagem | Tática | Evidência |
|-----------|--------|-----------|
| Gate: dados + sanidade + métrica ≥ ativo − tolerância (§6.3 item 12) | Checagem de sanidade | `treino/GateDePromocao.java:16-22` |
| Reprovado → REPROVADA, sem avanço do checkpoint, com limite K (ADR-011) | Monitoramento de condição | `treino/OrquestradorDoJob.java:124-125` (finaliza sem watermark; alerta em K), `:93` (para depois de K) |
| Transições de versão restritas (§6.4) | Prevenção de exceções | `treino/TransicoesDeVersao.java:10-22` |

- **Testes:** `GateDePromocaoTest` e `OrquestradorDoJobTest#rs09_*` ✅.
- **Ressalva:** o `AvaliadorDeCandidato` real não existe (Q-09). O gate compara uma pontuação abstrata.

### 4.7 AC-10: queda durante a promoção (A,M) → **Parcial**

| Abordagem | Tática | Evidência |
|-----------|--------|-----------|
| Transação final única: execução + checkpoint + ponteiro (ADR-010) | Transações | `treino/jdbc/RegistryDeModelosJdbc.java:112-141` (`setAutoCommit(false)` … `commit`/`rollback`) ✅ |
| Status das versões fora dessa transação | — | `treino/OrquestradorDoJob.java:133-134` mudam ATIVO e APROVADO_INATIVO **antes** do `finalizar` (`:135`), em chamadas separadas (DG-06) |
| Rollback manual | Reversão | `treino/PromotorDeModelo.java:31-41`: três escritas separadas, também fora de transação |

- **Testes:** `PersistenciaDoTreinoIntegracaoTest#rs10_cts10_02` ✅ prova o "tudo ou nada" do `finalizar`. **Nenhum teste injeta falha entre as linhas 133 e 135.**

---

## 5. Descobertas

### 5.1 Pontos de sensibilidade

| ID | Parâmetro ou decisão | Afeta | Evidência |
|----|----------------------|-------|-----------|
| SE-01 | Ordem "status das versões → `finalizar`" | Integridade do registry (AC-10) | `OrquestradorDoJob.java:133-135` |
| SE-02 | Margem Δ × timeout de transação da API | Perda de eventos (AC-08) | `OrquestradorDoJob.java:99`; o timeout não existe |
| SE-03 | Leitura `lerTodos()` antes de filtrar | Tempo do batch e da escrita de eventos (AC-07, AC-12) | `ExtratorIncremental.java:20`; `RegistradorDeEventos.java:13` |
| SE-04 | Chave HMAC gerada por instância | Sobrevivência das sessões a reinício e escala horizontal (AC-06) | `ServicoDeSessao.java:30` |
| SE-05 | Fronteira da vigência (início inclusivo, fim exclusivo) | Corretude temporal (AC-03) | `ServicoDeAssinatura.java:57-58`; AM-01 em aberto |
| SE-06 | Tolerância de reuso do refresh | Segurança × usabilidade (AC-04, AC-05) | `ServicoDeSessao.java:135` |

### 5.2 Pontos de trade-off

| ID | Trade-off | Ganha | Perde | Evidência |
|----|-----------|-------|-------|-----------|
| TO-01 | Token opaco validado na SESSAO a cada requisição | Revogação imediata (AC-04) | Uma consulta ao banco por requisição autenticada (desempenho) | `ServicoDeSessao.java:125` |
| TO-02 | Tolerância de reuso do refresh | Usabilidade de P2 (AC-05) | Uma janela curta para um refresh roubado (segurança) | `ServicoDeSessao.java:92-96,135` |
| TO-03 | Plano consultado a cada decisão | Corretude temporal (AC-03) | Uma consulta por pedido exclusivo (desempenho) | `PoliticaDeAcesso.java:30` |
| TO-04 | Não avançar o checkpoint quando o gate reprova | Nenhum dado perdido | Janelas acumuladas e mais custo (AC-12) | `OrquestradorDoJob.java:124` |
| TO-05 | Chave HMAC por instância | Nada a configurar; nenhum segredo no repositório | Sessões perdidas a cada reinício (AC-06) | `ServicoDeSessao.java:30` |

### 5.3 Riscos

**Confirmado** = a evidência no código mostra que a resposta do cenário não é produzida hoje. **Dúvida** = depende de medição ou de pergunta em aberto.

| ID | Risco | Tipo | Cenário | Direcionador | Evidência |
|----|-------|------|---------|--------------|-----------|
| RC-01 | Nenhuma regra de acesso alcança um usuário real: não há HTTP nem persistência | **Confirmado** | AC-01 | DN-01, DN-02 | 0 controladores, filtros ou repositórios de produção |
| RC-02 | O cancelamento não corta o acesso antes do fim da vigência | **Confirmado** (enquanto Q-03 não for respondida) | AC-03 | DN-01, DN-02 | `ServicoDeAssinatura.java:44-48,57-58` |
| RC-03 | O batch não tem isolamento de processo nem de recurso | **Confirmado** | AC-07 | DN-05 | Sem ponto de entrada do Job; `ProvisionadorDeBanco.java:83-92` sem limites |
| RC-04 | Uma queda entre a troca de status e o `finalizar` deixa o status de uma versão divergente do ponteiro | **Confirmado** (por inspeção; sem teste) | AC-10 | DN-05 | `OrquestradorDoJob.java:133-135`; `PromotorDeModelo.java:36-41` |
| RC-05 | Todas as sessões caem a cada reinício ou deploy | **Confirmado** | AC-06 | DN-03 | `ServicoDeSessao.java:30` |
| RC-06 | O custo do batch e da escrita de eventos cresce com o histórico, não com a janela | **Confirmado** no adaptador em memória; o adaptador real não existe | AC-12 | DN-05 | `ExtratorIncremental.java:20`; `RegistradorDeEventos.java:13` |
| RC-07 | Nenhuma ação sensível é auditada, e não há log nem métrica | **Confirmado** | AC-15 | DN-04, DN-02 | 0 loggers; nenhum componente de auditoria |
| RC-08 | A garantia contra eventos tardios depende de Δ e de um timeout que não existem | Dúvida | AC-08 | DN-05 | `OrquestradorDoJob.java:99` |
| RC-09 | A imagem `postgres:latest` sem versão fixa torna o CI não reprodutível | **Confirmado** (RK-10 continua aberto) | AC-14 | DN-04 | `TestcontainersConfiguration.java:15`; `PersistenciaDoTreinoIntegracaoTest.java:42` |
| RC-10 | A quarentena não é persistida: a linhagem dos rejeitados se perde | **Confirmado** | AC-16 | DN-05 | `ValidadorDeDados.java:21-29` devolve a lista, mas nenhum código grava em `ml.quarentena` |

### 5.4 Não riscos

| ID | Não risco | Cenário | Evidência |
|----|-----------|---------|-----------|
| NR-01 | A matriz de acesso é decidida num único ponto, que nega por padrão | AC-02 | `PoliticaDeAcesso.java:21-42`; `AcessoAoConteudoTest#rn19_ct19_03` |
| NR-02 | Execuções sobrepostas do Job | AC-11 | `GerenciadorDeLockPostgres.java:33-38`; `PersistenciaDoTreinoIntegracaoTest#rs05_*` contra PostgreSQL real |
| NR-03 | Papéis do banco isolam o batch de `app` | AC-13 | `ProvisionadorDeBanco.java:83-92`; `#rs15_cts15_01` (SQLSTATE 42501) |
| NR-04 | Ponteiro, checkpoint e execução mudam juntos | AC-10 (parte) | `RegistryDeModelosJdbc.java:112-141`; `#rs10_cts10_02` |
| NR-05 | Uma regressão numa regra coberta bloqueia o merge | AC-14 | Run `43511699150` ❌ (`expected: <402> but was: <403>`), depois `43512113294` ✅; proteção da `main` exige `verify (Java 21)` |
| NR-06 | Corrida de renovação na mesma sessão | AC-05 | `ServicoDeSessao.java:85-96`; `SessaoTest#rn08_*` com threads |
| NR-07 | Versão do modelo imutável no banco | AC-10 | `ProvisionadorDeBanco.java:55-62` (trigger); `#rs12_cts12_01` |

### 5.5 Temas de risco

| ID | Tema | Riscos | Direcionador ameaçado |
|----|------|--------|-----------------------|
| **TR-01** | **Núcleo sem sistema:** as regras existem e estão testadas, mas não há borda HTTP, persistência da API, ponto de entrada do Job nem configuração. A arquitetura documentada é a do sistema; o código é a do domínio. | RC-01, RC-03, RC-05, RC-06 | DN-01, DN-02, DN-05 |
| **TR-02** | **Atomicidade incompleta no registry:** a transação cobre ponteiro e checkpoint, mas não o estado das versões | RC-04 | DN-05 |
| **TR-03** | **Operação às cegas:** sem log, métrica nem auditoria, nenhum cenário de produção é verificável | RC-07, RC-08, RC-10 | DN-04 |
| **TR-04** | **Regras de negócio provisórias tratadas como comportamento:** o código já escolheu respostas para perguntas em aberto (cancelamento, bordas) | RC-02 | DN-01 |

---

## 6. Divergências: o que `docs/arquitetura.md` promete × o que o código faz

| ID | Promessa (documento) | Realidade (código) | Severidade |
|----|----------------------|--------------------|------------|
| **DG-01** | FiltroDeAutenticacao e camada HTTP (catálogo `:275`, componentes §2.4, sequência §4.3 a partir de `:625`) | Não existem. 0 controladores, filtros ou `@Configuration`. TratadorDeErros (`erro/TratadorDeErros.java:16-25`) não está ligado ao HTTP. | **Alta** |
| **DG-02** | Banco Operacional com app, sessão, assinatura e conteúdo; Spring Data JPA (§1.1, §3) | 0 adaptadores de produção para `Repositorio*`. JPA declarado no `pom.xml` e nunca usado. As tabelas de `app` são criadas pelo ProvisionadorDeBanco, mas ninguém as lê ou escreve. | **Alta** |
| **DG-03** | Job de Treino Diário em processo separado, disparado pelo Agendador Diário (DA-06 `:871`; ADR-006 `:1619`) | Nenhum ponto de entrada nem agendador. O orquestrador só é instanciado pelos testes. | **Alta** |
| **DG-04** | O ServicoDeConteudo registra eventos e aplica o Modelo M (§4.3 `:669-670`); o LeitorDoModeloAtivo recarrega periodicamente (DA-16 `:881`) | `RegistradorDeEventos` e `LeitorDoModeloAtivo` não são chamados por nenhum código de produção. `ServicoDeConteudo.java` não registra eventos. | Média |
| **DG-05** | `batch_leitor` com limite de conexões e `statement_timeout` (DA-13 `:878`) | `ProvisionadorDeBanco.java:83-92` concede privilégios, mas não define `CONNECTION LIMIT` nem `statement_timeout` | Média |
| **DG-06** | "Tudo ou nada" na promoção (ADR-010 `:1671`; DA-11 `:876`) | Status das versões fora da transação: `OrquestradorDoJob.java:133-135`; `PromotorDeModelo.java:36-41` | Média |
| **DG-07** | Cancelamento corta o acesso a partir do instante de corte (`:622`; ADR-003) | Só grava `canceladaEm` (`ServicoDeAssinatura.java:44-48`); `vigenteEm` ignora o campo (`:57-58`). Registrado como provisório em `plano-tdd.md` §7.4. | Média (depende de Q-03) |
| **DG-08** | Logs correlacionados, métricas, alertas e auditoria (§7 `:823-832`; ADR-016 `:1749`) | 0 loggers, 0 métricas, nenhum componente de auditoria | Média |
| **DG-09** | O batch lê só a janela (DA-07; §6.3 item 1) | `ExtratorIncremental.java:20` lê tudo e filtra em memória. `RegistradorDeEventos.java:13` faz o mesmo para deduplicar. | Média |
| **DG-10** | Registros inválidos vão para a QUARENTENA com o motivo (§6.3 item 8, `:799`) | `ValidadorDeDados.java:21-29` só separa a lista; nada grava em `ml.quarentena` (a tabela existe: `ProvisionadorDeBanco.java`) | Baixa |
| **DG-11** | Novas tentativas com **espera crescente** e timeout global da execução (§6.3 item 6, `:797`) | `OrquestradorDoJob.java:140-143` repete sem espera, e não há timeout global | Baixa |
| **DG-12** | Minimização: `sujeito_pseudonimo` nos eventos (§8 `:840`) | Nenhum código pseudonimiza: o campo vem pronto de quem chama o `RegistradorDeEventos` | Baixa (o registrador nem é usado, DG-04) |
| **DG-13** | TLS entre a Aplicação Cliente e a API (§8 `:843`) | Sem configuração de servidor nem de TLS (`application.properties:1`) | Baixa (sem HTTP, DG-01) |

**Convergências relevantes**, para não registrar só o que falta:
- ADR-002, ADR-003 (consulta ao plano), ADR-004, ADR-005, ADR-009 e ADR-012: implementados como descritos, com testes verdes no CI.
- Papéis do banco (§3) e imutabilidade de versão: implementados e testados contra PostgreSQL real.

---

## 7. Ações priorizadas para o próximo ciclo

Critério: primeiro o que destrava cenários (A,A); depois o que transforma "Parcial" em "Atende"; por fim o que dá evidência ao ciclo 02.

| Prioridade | Ação | Resolve | Cenários | Quem decide ou executa |
|------------|------|---------|----------|------------------------|
| **AP-01** | **Responder Q-03** (cancelamento: corte imediato ou no fim da vigência) e ajustar `ServicoDeAssinatura` e os casos CT-13.01/02 | RC-02, DG-07, TR-04 | AC-03 | Professor/PO → dev |
| **AP-02** | **Construir a borda HTTP mínima** (FiltroDeAutenticacao + rotas de conteúdo + TratadorDeErros ligado ao HTTP) e o **adaptador persistente** de sessão e assinatura; repetir a matriz §4.1 via HTTP | RC-01, DG-01, DG-02, TR-01 | AC-01, AC-04 | dev, com ADR-001/002/004 |
| **AP-03** | **Incluir a troca de status das versões na transação de `finalizar`** (e no rollback) e escrever o teste de injeção de falha entre as linhas 133 e 135 | RC-04, DG-06, TR-02 | AC-10 | dev |
| **AP-04** | **Ponto de entrada do Job** em processo separado + `CONNECTION LIMIT` e `statement_timeout` para o `batch_leitor`; depois executar o EXP-01 (carga com e sem batch) | RC-03, DG-03, DG-05 | AC-07 | dev + DevOps |
| **AP-05** | **Chave HMAC vinda de configuração secreta**, injetada no `ServicoDeSessao`, mantendo o CI sem segredos (o teste continua gerando a sua) | RC-05, SE-04 | AC-06 | dev + DevOps |
| **AP-06** | **Leitura da janela no banco** (`WHERE registrado_em > ? AND registrado_em <= ?`) num adaptador JDBC de eventos; a deduplicação passa a ser a restrição `UNIQUE` | RC-06, DG-09 | AC-12 | dev |
| **AP-07** | **Observabilidade mínima:** logs com `id_execucao` e `id_correlacao`, a métrica de eventos perdidos e o registro de promoção e rollback; responder Q-06 (auditoria) | RC-07, DG-08, TR-03 | AC-15 | Professor (Q-06) → dev |
| **AP-08** | **Fixar a versão da imagem do PostgreSQL** nos testes (`TestcontainersConfiguration.java:15`, `PersistenciaDoTreinoIntegracaoTest.java:42`) | RC-09 | AC-14 | dev |
| **AP-09** | **Persistir a quarentena** e implementar o backoff e o timeout global do Job | RC-10, DG-10, DG-11 | AC-16 | dev |
| **AP-10** | **Fazer a fase 2 do ATAM com stakeholders:** validar as personas (C-03), votar importância e dificuldade, responder Q-08, Q-09, Q-10 e Q-14 | Limitações deste ciclo | Todos | Professor, turma |

---

## 8. Limitações deste ciclo

1. **Sem stakeholders:** importância e dificuldade são propostas do avaliador (passos 7–8 do ATAM não realizados).
2. **Personas geradas por IA** (`docs/prd.md:8`). DN-03 depende delas.
3. **Nenhum requisito numérico existe.** As medidas com faixa (AC-07) são hipóteses.
4. **Nenhuma medição de desempenho foi feita.** Os experimentos EXP-01 e EXP-02 continuam pendentes.
5. **O comportamento "401 para tudo"** do sistema em execução foi observado no setup e **não foi reverificado** neste ciclo.
6. **As evidências de execução são os runs de CI citados.** A análise de código é estática (leitura e busca), e nenhuma regra foi testada além da suíte existente.
