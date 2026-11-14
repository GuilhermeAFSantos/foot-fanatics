# Foot Fanatics — Plano de TDD (inventário de regras e casos de teste)

> **Status:** fase **green** concluída em 2026-11-13. `./mvnw -B verify` passa: 199 testes, 0 falhas, 0 erros, 36 desabilitados. Ver a §7 (matriz atual). A §6 fica como registro da fase red.
>
> ~~Status anterior: fase red concluída em 2026-11-13; testes escritos e assinaturas mínimas sem lógica.~~
>
> ~~Status anterior: preparação do ciclo de TDD. Nenhum teste nem código de produção foi escrito.~~
> **Fonte de verdade:** só os artefatos do repositório. Nenhuma regra foi criada aqui; cada uma cita a origem como `arquivo:linha` ou ADR.

## 0. Como ler este plano

### 0.1 Artefatos consultados e onde procurei

| Procurado | Onde procurei | Encontrado |
|-----------|---------------|------------|
| Personas | `docs/prd.md` | §3, linhas 6–26 (P1–P3, **geradas por IA, validação pendente**: `docs/prd.md:8`) |
| Requisitos com ID (RF/RNF/RN/BR) | `docs/`, `src/`, `README.md` (busca por `RN-`, `RF-`, `RNF-`, `BR-` seguidos de número) | **Nenhum.** Já registrado como lacuna L-02 em `docs/arquitetura.md` |
| Escopos | `docs/prd.md:27-33`; `README.md:9-14` | E1–E4 e o fora de escopo |
| Regras de negócio explícitas | `docs/` (busca por "regra"); arquivos `*regra*`, `*rule*`, `*.feature` fora de `.aiox-core/` | **Nenhum documento de regras.** Os achados em `.claude/` são ferramental do AIOX, não do produto. |
| Stories | `docs/stories/` | **Pasta inexistente** |
| Arquitetura e ADRs | `docs/arquitetura.md` | §4.1 matriz de acesso (`:599-611`), §4.2 ciclo de sessão (`:613-623`), §3 papéis (`:587-589`), §6 pipeline (`:707-825`), ADR-001 a ADR-016 (`:1554-1762`) |
| Plano de teste | `docs/plano-de-teste.md` | Todas as seções em `TODO` |
| Código de produção | `src/main/` | Só `FootFanaticsApplication.java` e `application.properties` (nome da aplicação). **Nenhuma regra implementada.** |
| Testes existentes | `src/test/` | `FootFanaticsApplicationTests` (só verifica se o contexto sobe), `TestcontainersConfiguration`, `TestFootFanaticsApplication`. **Nenhum teste de regra.** |

### 0.2 Força da origem

Como não há requisitos com ID, as regras vêm de três tipos de fonte. **A força da origem decide se o caso já pode entrar no ciclo de TDD.**

| Força | Fonte | O que significa para o TDD |
|-------|-------|----------------------------|
| **A — Escopo** | `README.md:9-12`, `docs/prd.md:27-33` | Fato. Muitas vezes vago demais para dar um resultado esperado exato. |
| **B — Decisão aceita** | ADR com status **Aceita** em `docs/arquitetura.md` | Pode entrar no ciclo. |
| **C — Decisão proposta** | ADR **Proposta**, ou seção da Parte B ligada a ela | Os casos estão escritos, mas são **condicionais**: confirmar a ADR antes de escrever o teste. |

**Situação de cada caso:**
- **Pronto:** origem A ou B e resultado esperado definido.
- **Condicional:** depende de uma ADR Proposta.
- **Bloqueado:** depende de pergunta aberta (Q-xx, QT-xx) ou de parâmetro sem valor.

### 0.3 Parâmetros sem valor

Nenhum artefato define valores para os parâmetros abaixo. Os casos-limite usam o **nome** do parâmetro (por exemplo, `TTL_acesso − 1 ms`), nunca um número inventado.

| Parâmetro | Significado | Pendência |
|-----------|-------------|-----------|
| `TTL_acesso` / `TTL_refresh` | Vida do token de acesso / do refresh | Q-04 |
| `T_tol` | Tolerância de reuso do refresh anterior | EXP-10 (ADR-004) |
| `T_rec` | Validade do código de recuperação | L-05 |
| `Δ` | Margem do corte da janela | Q-15 (ADR-008) |
| `K` | Reprovações consecutivas até a escalação | ADR-011 |
| `L_quar` | Limite da taxa de quarentena | §6.3 item 8 |
| `tol_gate` / `piso` | Tolerância e piso do gate | Q-09 |
| `t_corte` | Instante em que o cancelamento corta o acesso | Q-03 |

### 0.4 Tipos de caso e identificadores

- **Tipos de caso:** F = caminho feliz · L = valor-limite · I = entrada inválida · C = conflito · P = estado proibido.
- **Identificadores:**
  - **RN-xx** = regra de negócio (E1–E4);
  - **RS-xx** = regra do pipeline diário (restrição R-03 do professor; separada porque não é regra de negócio do produto);
  - **CT-xx.nn** = caso de teste da regra xx;
  - **AM-xx** = ambiguidade;
  - **QT-xx** = pergunta nova deste plano;
  - Q-xx e L-xx são as perguntas e lacunas já existentes em `docs/arquitetura.md` §A10–A12.

---

## 1. Inventário das regras

| ID | Regra (como está no artefato) | Origem | Força | Escopo |
|----|-------------------------------|--------|-------|--------|
| RN-01 | Existe cadastro de conta. | `README.md:9` | A | E1 |
| RN-02 | A credencial é guardada só como hash adaptativo. | `docs/arquitetura.md` §1.1 ("Hash de senha adaptativo… Adotado o mecanismo"); ADR-015 | B (mecanismo) / C (ADR-015) | E1 |
| RN-03 | Existe perfil de usuário. | `README.md:9` | A | E1 |
| RN-04 | Recuperação de acesso: código de uso único e curto, guardado como hash; mesma resposta para e-mail existente ou inexistente; o sucesso revoga as sessões existentes. | `README.md:9`; `docs/arquitetura.md:623`; ADR-015 | C | E1 |
| RN-05 | O login cria uma SESSAO por dispositivo e emite token de acesso + refresh token (este guardado só como hash). | `README.md:10`; `docs/arquitetura.md:617`; ADR-004 | C | E2 |
| RN-06 | ~~Token de acesso expirado → 401 `token_expirado`; malformado ou revogado → 401 `token_invalido`.~~ **(Aula 07, §9)** Ausente → 401 `token_ausente`; assinatura inválida → 401 `token_invalido`; expiração ≤ agora → 401 `token_expirado`; revogado → 401 `token_revogado`; só válido e vigente prossegue, mesmo com assinatura ativa. O token é opaco e validado contra SESSAO a cada requisição. | `README.md:10`; `docs/arquitetura.md:618`; ADR-004, ADR-014; **cenário E2 da Aula 07** | C | E2 |
| RN-07 | A renovação troca o refresh por um novo (rotação). O refresh imediatamente anterior, reapresentado pelo mesmo dispositivo dentro da tolerância, devolve o mesmo par. Fora disso, revoga a sessão. | `README.md:10`; `docs/arquitetura.md:619`; ADR-004 | C | E2 |
| RN-08 | Na renovação concorrente da mesma sessão, só uma vence (`versao`), e a outra, dentro da tolerância, recebe o mesmo par. Cada dispositivo tem sua própria SESSAO. | `docs/arquitetura.md:620`; ADR-004 | C | E2 |
| RN-09 | O logout revoga a SESSAO do dispositivo atual, e o token deixa de valer na hora. | `README.md:10`; `docs/arquitetura.md:621`; ADR-004 (atraso 0) | C | E2 |
| RN-10 | Os planos são **free** e **premium**. | `README.md:11` | A | E3 |
| RN-11 | O premium dá acesso ao conteúdo exclusivo **somente durante a vigência**. | `README.md:11-12`; `docs/arquitetura.md:606`; ADR-003, ADR-005 | B | E3 |
| RN-12 | Existe upgrade free → premium. | `README.md:11` | A | E3 |
| RN-13 | Cancelamento ou vencimento corta o acesso exclusivo a partir do instante de corte, sem mexer na sessão. | `README.md:11`; `docs/arquitetura.md:622`; ADR-003 | B (mecanismo) / bloqueado (`t_corte`, Q-03) | E3 |
| RN-14 | O plano é consultado a cada decisão de conteúdo exclusivo, nunca lido do token. | ADR-003 | B | E3/E4 |
| RN-15 | Conteúdo público é permitido para anônimo, free, premium vigente e premium cancelado ou vencido. | `README.md:12`; `docs/arquitetura.md:605` | B | E4 |
| RN-16 | Conteúdo exclusivo: anônimo → 401 `autenticacao_necessaria`; free → 403 `plano_insuficiente`; premium vigente → permitido; premium cancelado ou vencido → 403 `plano_insuficiente`. | `README.md:12`; `docs/arquitetura.md:606`; ADR-002 | B (códigos de erro: C, ADR-014) | E4 |
| RN-17 | Conta, sessão e assinatura do próprio usuário: anônimo → 401; autenticado → permitido. | `docs/arquitetura.md:607`; ADR-002 | B | E1–E3 |
| RN-18 | Dados de outro usuário: anônimo → 401; autenticado (qualquer plano) → 403 `acesso_negado`. | `docs/arquitetura.md:608`; ADR-002 | B | E1–E3 |
| RN-19 | Negar por padrão: sem regra explícita, o acesso é negado. O ServicoDeConteudo consulta a PoliticaDeAcesso antes de devolver **qualquer** item. | ADR-002 | B | E4 |
| RN-20 | O domínio tem clubes, jogos, resultados e matérias. | `README.md:3,12` | A | E4 |
| RN-21 | Cada situação de erro tem um código estável no formato Problem Details. | ADR-014; `docs/arquitetura.md:605-623` | C | Transversal |
| RS-01 | O Job treina só com dados novos: janela (watermark, corte], com corte = início − Δ, por tempo de ingestão. | R-03 (PROF); `docs/arquitetura.md:792,798`; ADR-008 | C | Pipeline |
| RS-02 | O checkpoint só avança na transação final, junto com a execução CONCLUIDA (ou DESCARTADA). | `docs/arquitetura.md:793,714-722`; ADR-010, ADR-011; INV-2 | B | Pipeline |
| RS-03 | Idempotência por `data_referencia`: CONCLUIDA → sem efeito; FALHOU → retoma com a mesma janela. | `docs/arquitetura.md:794`; ADR-009 | B | Pipeline |
| RS-04 | Deduplicação por `id_evento`. | `docs/arquitetura.md:795`; ADR-007 | C (chave de negócio bloqueada, Q-09) | Pipeline |
| RS-05 | No máximo uma execução ativa (lock). | `docs/arquitetura.md:796`; ADR-009 | B | Pipeline |
| RS-06 | Falha → execução FALHOU com checkpoint intacto; órfãos arquivados pelo OrquestradorDoJob no início da execução seguinte. | `docs/arquitetura.md:797`; ADR-010 | B | Pipeline |
| RS-07 | Validação de dados: esquemas N e N−1 aceitos; esquema desconhecido → quarentena; taxa de quarentena acima do limite → FALHOU. | `docs/arquitetura.md:799`; ADR-007 | C | Pipeline |
| RS-08 | O gate aprova se dados e sanidade passarem e métrica do candidato ≥ métrica do ativo − tolerância. Na primeira execução, valem sanidade + piso. | `docs/arquitetura.md:803`; INV-3 | B (mecanismo) / bloqueado (Q-09) | Pipeline |
| RS-09 | Gate reprova → versão REPROVADO, execução REPROVADA, checkpoint mantido. Após K reprovações consecutivas, o Job para e alerta. DESCARTADA só por decisão humana auditada. | `docs/arquitetura.md:714-722`; ADR-011 | C | Pipeline |
| RS-10 | Promoção atômica: ponteiro, execução e checkpoint na mesma transação. | `docs/arquitetura.md:793`; ADR-010 | B | Pipeline |
| RS-11 | Só o PromotorDeModelo altera o ponteiro. O rollback aponta para uma versão APROVADO_INATIVO. | `docs/arquitetura.md:804,809-820`; ADR-010 | B | Pipeline |
| RS-12 | A VERSAO_MODELO é imutável depois de registrada. | `docs/arquitetura.md:802`; ADR-010 | B | Pipeline |
| RS-13 | O LeitorDoModeloAtivo só troca de versão após carga válida; senão mantém a atual, ou faz fallback sem modelo. | `docs/arquitetura.md:805`; ADR-013 | C | Pipeline/API |
| RS-14 | Candidato = modelo ativo + dados da janela (treino incremental). | `docs/arquitetura.md:807`; ADR-012 | B | Pipeline |
| RS-15 | O papel `batch_leitor` só lê `eventos` e não tem acesso a `app`. | `docs/arquitetura.md:587-589`; ADR-015 | C | Pipeline |

> **Pré-requisito de testabilidade (não é regra de negócio):** ADR-005 (Aceita) manda que toda regra de tempo obtenha "agora" do Relogio. Todos os casos-limite temporais abaixo pressupõem um Relogio fixável no teste. Sem ele, os casos L não são determinísticos.

---

## 2. Casos de teste: regras de negócio (E1–E4)

### RN-01 — Cadastro de conta · `README.md:9` · A
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-01.01 | F | Cadastro com dados válidos. | Conta criada; usuário consegue fazer login (RN-05). | **Bloqueado:** quais campos são obrigatórios e o que é "válido" não está definido (LG-01) |
| CT-01.02 | I | Cadastro com campo obrigatório ausente. | Rejeitado com erro de validação. | **Bloqueado:** campos e código de erro indefinidos (LG-01, AM-03) |
| CT-01.03 | C | Cadastro com e-mail já usado. | — | **Bloqueado:** a unicidade do e-mail não está escrita em lugar nenhum (QT-01) |
| CT-01.04 | P | Conta criada sem credencial. | Não pode existir. | **Bloqueado:** a regra só existe implicitamente (LG-01) |

### RN-02 — Credencial guardada só como hash · §1.1; ADR-015 · B/C
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-02.01 | F | Cadastro seguido de leitura da credencial armazenada. | O valor armazenado ≠ a senha informada e é verificável contra ela. | Pronto (mecanismo) |
| CT-02.02 | F | Login com a senha correta. | Autentica (RN-05). | Pronto |
| CT-02.03 | I | Login com a senha errada. | Não autentica. | **Bloqueado:** código e corpo da resposta de falha de login indefinidos (AM-03) |
| CT-02.04 | P | Senha em texto puro em qualquer coluna ou log. | Nunca ocorre. | Condicional (ADR-015: "nenhum token, senha ou e-mail em log") |
| CT-02.05 | I | Senha que não atende a política. | — | **Bloqueado:** sem política de credencial (L-06) |

### RN-03 — Perfil · `README.md:9` · A
Nenhum caso possível. O artefato só diz que "perfil" existe, sem nenhuma regra (`docs/arquitetura.md` D6.1, item 1). Ver LG-02.

### RN-04 — Recuperação de acesso · `docs/arquitetura.md:623`; ADR-015 · C
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-04.01 | F | Pedir recuperação para e-mail existente e usar o código dentro da validade. | Acesso recuperado; **todas** as sessões anteriores revogadas (tokens antigos → 401 `token_invalido`). | Condicional |
| CT-04.02 | C | Pedir recuperação para e-mail **inexistente**. | Resposta **idêntica** à do e-mail existente (status e corpo). | Condicional |
| CT-04.03 | P | Usar o mesmo código uma segunda vez. | Rejeitado. | Condicional |
| CT-04.04 | L | Usar o código em `T_rec − 1 ms` / `T_rec` / `T_rec + 1 ms` após a emissão. | Aceito / **ambíguo** (AM-01) / rejeitado. | **Bloqueado:** `T_rec` (L-05) |
| CT-04.05 | I | Código adulterado ou de outra conta. | Rejeitado, sem revogar sessões. | Condicional |
| CT-04.06 | P | Código guardado em texto puro. | Nunca ocorre (só hash). | Condicional |
| CT-04.07 | C | Dois códigos válidos emitidos em sequência para a mesma conta. | — | **Bloqueado:** não se sabe se o novo invalida o anterior (QT-02) |

### RN-05 — Login cria sessão por dispositivo · `docs/arquitetura.md:617`; ADR-004 · C
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-05.01 | F | Login com credencial válida. | Uma SESSAO criada; token de acesso + refresh emitidos; refresh guardado só como hash. | Condicional |
| CT-05.02 | F | Login do mesmo usuário em dois dispositivos. | Duas SESSAO independentes, cada uma com seu par de tokens. | Condicional |
| CT-05.03 | I | Login com credencial inválida. | Nenhuma SESSAO criada. | Condicional (o código de resposta é AM-03) |
| CT-05.04 | C | Novo login no **mesmo** dispositivo com uma sessão ativa. | — | **Bloqueado:** não se sabe se substitui ou cria outra sessão (QT-03, AM-10) |
| CT-05.05 | C | Login além de um número máximo de sessões. | — | **Bloqueado:** limite de sessões indefinido (Q-05) |

### RN-06 — Validação do token de acesso · `docs/arquitetura.md:618`; ADR-004, ADR-014 · C
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-06.01 | F | Requisição autenticada com token válido. | Principal autenticado; a requisição segue. | Condicional |
| CT-06.02 | L | Requisição em `TTL_acesso − 1 ms`. | Aceita. | **Bloqueado:** `TTL_acesso` (Q-04) |
| CT-06.03 | L | Requisição exatamente em `TTL_acesso`. | 401 `token_expirado` (Aula 07, §9; antes ambíguo, AM-01) | Pronto |
| CT-06.04 | L | Requisição em `TTL_acesso + 1 ms`. | 401 `token_expirado`. | **Bloqueado:** `TTL_acesso` |
| CT-06.05 | I | Token malformado. | 401 `token_invalido`. | Condicional |
| CT-06.06 | I | Token inexistente (formato válido, sem SESSAO). | 401 `token_invalido`. | Condicional |
| CT-06.07 | P | Token de sessão revogada. | 401 `token_invalido`, nunca 200. | Condicional |
| CT-06.08 | C | Token expirado **e** de sessão revogada. | 401 `token_expirado` (Aula 07, §9; antes QT-04) | Pronto |

### RN-07 — Renovação com rotação e tolerância · `docs/arquitetura.md:619`; ADR-004 · C
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-07.01 | F | Renovar com o refresh atual. | Novo par emitido; o refresh anterior deixa de ser o atual; `versao` incrementada. | Condicional |
| CT-07.02 | L | Reapresentar o refresh anterior, do mesmo dispositivo, em `T_tol − 1 ms`. | Devolve **o mesmo par** já emitido; a sessão continua. | **Bloqueado:** `T_tol` (EXP-10) |
| CT-07.03 | L | O mesmo, em `T_tol + 1 ms`. | Sessão revogada; os dois tokens → 401 `token_invalido`. | **Bloqueado:** `T_tol` |
| CT-07.04 | C | Refresh anterior, dentro de `T_tol`, de **outro** dispositivo. | Sessão revogada. | Condicional; **AM-10** (como se identifica "dispositivo") |
| CT-07.05 | P | Reapresentar um refresh **mais antigo** que o anterior. | Sessão revogada. | Condicional |
| CT-07.06 | L | Renovar em `TTL_refresh − 1 ms` / `TTL_refresh + 1 ms`. | Aceita / rejeitada. | **Bloqueado:** `TTL_refresh` (Q-04) |
| CT-07.07 | I | Refresh adulterado ou de sessão revogada. | 401 `token_invalido`; nenhum par emitido. | Condicional |
| CT-07.08 | P | Dois pares **distintos** válidos ao mesmo tempo para a mesma sessão. | Nunca ocorre. | Condicional |

### RN-08 — Renovação concorrente · `docs/arquitetura.md:620`; ADR-004 · C
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-08.01 | C | Duas requisições simultâneas com o mesmo refresh, mesma sessão. | Uma vence (`versao` +1); a outra recebe **o mesmo par**; a sessão continua ativa. | Condicional (depende de `T_tol`) |
| CT-08.02 | F | Renovações simultâneas em **dispositivos diferentes**. | Ambas vencem, cada uma na própria SESSAO. | Condicional |
| CT-08.03 | P | Após a corrida, `versao` incrementada duas vezes ou dois pares distintos válidos. | Nunca ocorre. | Condicional |

### RN-09 — Logout · `docs/arquitetura.md:621`; ADR-004 · C
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-09.01 | F | Logout e, logo depois, uso do mesmo token de acesso. | 401 `token_invalido` **imediatamente** (atraso 0, ADR-004). | Condicional |
| CT-09.02 | F | Logout e uso do refresh da mesma sessão. | Rejeitado; nenhum par emitido. | Condicional |
| CT-09.03 | C | Logout no dispositivo A com sessão ativa no dispositivo B. | B continua válida (a regra diz "do dispositivo atual"). | Condicional; **Q-05** (logout global) pode mudar |
| CT-09.04 | I | Logout com token já revogado ou inválido. | — | **Bloqueado:** resposta indefinida (QT-05) |
| CT-09.05 | P | Sessão revogada que volta a aceitar tokens. | Nunca ocorre. | Condicional |

### RN-10 — Planos free e premium · `README.md:11` · A
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-10.01 | F | Atribuir o plano free / premium. | Aceito. | **Bloqueado:** não se sabe se todo cadastro começa no free (QT-06) |
| CT-10.02 | I | Atribuir um plano diferente de free e premium. | Rejeitado. | Pronto (só existem esses dois: `README.md:11`) |
| CT-10.03 | P | Usuário com dois planos vigentes ao mesmo tempo. | — | **Bloqueado:** não está escrito (QT-07) |

### RN-11 — Acesso premium só durante a vigência · `docs/arquitetura.md:606`; ADR-003, ADR-005 · B
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-11.01 | F | Premium com `inicio_vigencia` < agora < `fim_vigencia` pede conteúdo exclusivo. | Permitido. | **Pronto** |
| CT-11.02 | L | Agora = `inicio_vigencia − 1 ms`. | 403 `plano_insuficiente`. | **Pronto** |
| CT-11.03 | L | Agora = `inicio_vigencia`. | **Ambíguo** (AM-01). | Bloqueado |
| CT-11.04 | L | Agora = `fim_vigencia − 1 ms`. | Permitido. | **Pronto** |
| CT-11.05 | L | Agora = `fim_vigencia`. | **Ambíguo** (AM-01). | Bloqueado |
| CT-11.06 | L | Agora = `fim_vigencia + 1 ms`. | 403 `plano_insuficiente`. | **Pronto** |
| CT-11.07 | I | Assinatura com `fim_vigencia` < `inicio_vigencia`. | — | **Bloqueado:** a validação não está escrita (QT-08) |

### RN-12 — Upgrade free → premium · `README.md:11` · A
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-12.01 | F | Usuário free faz upgrade e pede conteúdo exclusivo. | Permitido (consequência de RN-11 e RN-14, sem novo login). | **Bloqueado:** o mecanismo do upgrade é desconhecido (Q-02; S-04) |
| CT-12.02 | C | Usuário premium vigente pede upgrade. | — | **Bloqueado** (QT-09) |
| CT-12.03 | P | Upgrade que gera duas assinaturas vigentes. | — | **Bloqueado** (QT-07) |

### RN-13 — Cancelamento ou vencimento corta o acesso · `docs/arquitetura.md:622`; ADR-003 · B / bloqueado
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-13.01 | F | Premium cancela; pedido de exclusivo após `t_corte`. | 403 `plano_insuficiente`, **com o mesmo token** (a sessão continua válida). | **Bloqueado:** `t_corte` (Q-03: imediato ou fim da vigência?) |
| CT-13.02 | L | Pedido de exclusivo em `t_corte − 1 ms` / `t_corte + 1 ms`. | Permitido / 403. | **Bloqueado:** Q-03; AM-01 para `= t_corte` |
| CT-13.03 | F | Após o cancelamento, pedido de conteúdo público. | Permitido (RN-15). | **Pronto** |
| CT-13.04 | F | Após o cancelamento, pedido dos próprios dados de conta. | Permitido; a sessão não foi revogada. | **Pronto** |
| CT-13.05 | I | Usuário free tenta cancelar. | — | **Bloqueado** (QT-10) |
| CT-13.06 | C | Cancelar duas vezes. | — | **Bloqueado** (QT-10) |

### RN-14 — Plano consultado na hora, nunca lido do token · ADR-003 · B
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-14.01 | P | Token emitido enquanto premium vigente; vigência termina; o **mesmo** token pede exclusivo. | 403 `plano_insuficiente` (o token não carrega privilégio). | **Pronto** |
| CT-14.02 | F | Token emitido enquanto free; upgrade concluído; o **mesmo** token pede exclusivo. | Permitido sem novo login. | Bloqueado pelo mecanismo de upgrade (Q-02) |

### RN-15 — Conteúdo público permitido a todos · `docs/arquitetura.md:605` · B
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-15.01 | F | Anônimo pede conteúdo público. | Permitido. | **Pronto** |
| CT-15.02 | F | Free / premium vigente / premium cancelado pedem conteúdo público. | Permitido nos três. | **Pronto** |
| CT-15.03 | C | Token **inválido** ou expirado em pedido de conteúdo público. | — | **Bloqueado:** 401 ou tratar como anônimo? (QT-11) |

### RN-16 — Conteúdo exclusivo por perfil · `docs/arquitetura.md:606`; ADR-002 · B
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-16.01 | F | Premium vigente pede matéria exclusiva. | Permitido, com o conteúdo. | **Pronto** |
| CT-16.02 | I | Anônimo pede matéria exclusiva. | 401 `autenticacao_necessaria`. | **Pronto** (o nome do código é condicional, ADR-014) |
| CT-16.03 | I | Free pede matéria exclusiva. | 403 `plano_insuficiente`. | **Pronto** (o nome do código é condicional) |
| CT-16.04 | I | Premium vencido pede matéria exclusiva. | 403 `plano_insuficiente`. | **Pronto** |
| CT-16.05 | P | Resposta 401/403 que contenha **qualquer campo** do conteúdo exclusivo (título, corpo etc.). | Nunca ocorre. | **Pronto** (CEN-01: "sem nenhum campo do conteúdo exclusivo no corpo") |
| CT-16.06 | P | Lista mista de conteúdos (públicos + exclusivos) pedida por um free. | Os itens exclusivos não aparecem com conteúdo. | **Bloqueado:** não se sabe se eles são omitidos ou listados sem corpo (QT-12) |
| CT-16.07 | C | O que o free vê a mais que o anônimo. | — | **Bloqueado** (Q-19) |

### RN-17 — Próprios dados de conta, sessão e assinatura · `docs/arquitetura.md:607` · B
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-17.01 | F | Usuário autenticado (free ou premium) consulta a própria assinatura. | Permitido. | **Pronto** |
| CT-17.02 | I | Anônimo consulta conta, sessão ou assinatura. | 401. | **Pronto** (o código específico não está definido na matriz: AM-03) |
| CT-17.03 | F | Premium cancelado consulta a própria assinatura. | Permitido. | **Pronto** |

### RN-18 — Dados de outro usuário · `docs/arquitetura.md:608` · B
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-18.01 | P | Free pede os dados de assinatura de outro usuário (trocando o identificador). | 403 `acesso_negado`, sem dado do outro usuário no corpo. | **Pronto** |
| CT-18.02 | P | Premium vigente faz o mesmo. | 403 `acesso_negado`. | **Pronto** |
| CT-18.03 | I | Anônimo faz o mesmo. | 401. | **Pronto** |
| CT-18.04 | C | Pedido de um identificador de usuário **inexistente**. | — | **Bloqueado:** 403 ou 404? O 404 revelaria a existência de contas (QT-13) |

### RN-19 — Negar por padrão · ADR-002 · B
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-19.01 | P | Conteúdo com classificação ausente ou desconhecida, pedido por premium vigente. | Negado. | **Pronto** |
| CT-19.02 | P | Um caminho do ServicoDeConteudo que devolva item sem passar pela PoliticaDeAcesso. | Nunca ocorre. É verificável por teste de arquitetura ou por cobertura da matriz. | **Pronto** |
| CT-19.03 | F | Matriz §4.1 completa: perfil × classificação × endpoint de conteúdo. | 100% das células com o resultado de RN-15 a RN-18. | **Pronto**, exceto as células do free (Q-19) |

### RN-20 — Domínio: clubes, jogos, resultados e matérias · `README.md:3,12` · A
Nenhum caso de regra possível. Não há nenhuma regra sobre esses dados: validação de placar, status de jogo, relação jogo × clube etc. Ver LG-03 e LG-04.

### RN-21 — Contrato de erro estável · ADR-014 · C
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-21.01 | F | Cada situação de RN-06, RN-16 e RN-18. | Corpo no formato Problem Details com o código documentado. | Condicional (Q-07) |
| CT-21.02 | P | A mesma situação devolvendo códigos diferentes em endpoints diferentes. | Nunca ocorre. | Condicional |
| CT-21.03 | P | Corpo de erro com detalhe interno (stack trace, SQL). | Nunca ocorre. | Condicional |

---

## 3. Casos de teste: regras do pipeline diário (R-03)

### RS-01 — Só dados novos, janela (watermark, corte] · `docs/arquitetura.md:792,798`; ADR-008 · C
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-S01.01 | F | Eventos com `registrado_em` entre watermark e corte. | Todos entram na janela. | Condicional |
| CT-S01.02 | L | Evento com `registrado_em` = watermark. | **Fora** (intervalo aberto à esquerda). | Condicional |
| CT-S01.03 | L | Evento com `registrado_em` = watermark + 1 ms. | Dentro. | Condicional |
| CT-S01.04 | L | Evento com `registrado_em` = corte. | **Dentro** (fechado à direita). | Condicional |
| CT-S01.05 | L | Evento com `registrado_em` = corte + 1 ms. | Fora (entra na próxima janela). | Condicional |
| CT-S01.06 | C | Evento com `ocorrido_em` antigo e `registrado_em` novo (tardio). | Dentro: a janela é por tempo de ingestão. | Condicional |
| CT-S01.07 | P | Evento já incluído numa execução CONCLUIDA reaparecendo numa janela posterior. | Nunca ocorre. | Condicional |
| CT-S01.08 | L | Corte calculado com `Δ`. | corte = início − Δ. | **Bloqueado:** `Δ` (Q-15) |

### RS-02 — Checkpoint só na transação final · `docs/arquitetura.md:793`; ADR-010, ADR-011 · B
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-S02.01 | F | Execução CONCLUIDA. | Checkpoint = corte da execução. | **Pronto** |
| CT-S02.02 | P | Execução FALHOU em qualquer etapa. | Checkpoint inalterado. | **Pronto** |
| CT-S02.03 | P | Execução REPROVADA. | Checkpoint inalterado. | Condicional (ADR-011) |
| CT-S02.04 | F | Janela marcada DESCARTADA por decisão humana auditada. | Checkpoint = corte, com registro de auditoria. | Condicional (ADR-011) |
| CT-S02.05 | P | Checkpoint avançado sem execução CONCLUIDA ou DESCARTADA correspondente. | Nunca ocorre. | **Pronto** |

### RS-03 — Idempotência por data · `docs/arquitetura.md:794`; ADR-009 · B
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-S03.01 | C | Reexecutar uma data CONCLUIDA. | Nenhuma escrita em `ml`; encerra sem efeito. | **Pronto** |
| CT-S03.02 | F | Reexecutar uma data FALHOU. | Retoma com a mesma janela; `hash_dados` igual ao da tentativa anterior. | **Pronto** |
| CT-S03.03 | P | Duas EXECUCAO_TREINO com a mesma `data_referencia`. | Nunca ocorre (unicidade). | **Pronto** |

### RS-04 — Deduplicação · `docs/arquitetura.md:795`; ADR-007 · C
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-S04.01 | P | Gravar duas vezes o mesmo `id_evento`. | Só uma linha existe. | Condicional |
| CT-S04.02 | F | Janela contendo o mesmo `id_evento` (por qualquer caminho). | Contado uma vez. | Condicional |
| CT-S04.03 | C | Mesmo fato com `id_evento` diferente. | — | **Bloqueado:** chave de negócio (Q-09; RA-02) |

### RS-05 — Uma execução por vez · `docs/arquitetura.md:796`; ADR-009 · B
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-S05.01 | C | Dois disparos simultâneos. | Um prossegue; o outro encerra sem efeito e registra log. | **Pronto** |
| CT-S05.02 | F | Processo cai com o lock. | O lock é liberado; a próxima execução obtém o lock. | **Pronto** |
| CT-S05.03 | P | Duas execuções EM_EXECUCAO ao mesmo tempo. | Nunca ocorre. | **Pronto** |

### RS-06 — Falha e órfãos · `docs/arquitetura.md:797`; ADR-010 · B
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-S06.01 | F | Falha depois de registrar um CANDIDATO e antes do gate. | Execução FALHOU; checkpoint e ponteiro intactos. | **Pronto** |
| CT-S06.02 | F | Execução seguinte, com o lock. | O candidato órfão vai para ARQUIVADO antes de qualquer treino. | **Pronto** |
| CT-S06.03 | I | Erro transitório (conexão). | Nova tentativa limitada; depois FALHOU. | **Bloqueado:** número de tentativas indefinido |
| CT-S06.04 | I | Erro de dados. | Não há nova tentativa; FALHOU direto. | **Pronto** |

### RS-07 — Validação de dados e esquema · `docs/arquitetura.md:799`; ADR-007 · C
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-S07.01 | F | Eventos com `versao_esquema` N e N−1. | Aceitos. | Condicional |
| CT-S07.02 | I | Evento com esquema N+1 ou desconhecido. | Vai para a QUARENTENA com o motivo. | Condicional |
| CT-S07.03 | L | Taxa de quarentena = `L_quar − ε` / `L_quar` / `L_quar + ε`. | Prossegue / **ambíguo** (AM-02) / FALHOU. | **Bloqueado:** `L_quar` |
| CT-S07.04 | P | Treino que consome evento de esquema desconhecido. | Nunca ocorre. | Condicional |
| CT-S07.05 | L | Janela com volume abaixo do mínimo. | FALHOU. | **Bloqueado:** volume mínimo indefinido |

### RS-08 — Gate de promoção · `docs/arquitetura.md:803` · B/bloqueado
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-S08.01 | F | Candidato com métrica > ativo; dados e sanidade ok. | Aprovado. | Bloqueado (métrica, Q-09) |
| CT-S08.02 | L | Métrica do candidato = ativo − `tol_gate`. | Aprovado (a regra é "≥"). | Bloqueado (`tol_gate`) |
| CT-S08.03 | L | Métrica do candidato = ativo − `tol_gate` − ε. | Reprovado. | Bloqueado |
| CT-S08.04 | P | Sanidade reprovada (artefato não carrega), mesmo com métrica melhor. | Reprovado. | **Pronto** (a sanidade é condição obrigatória) |
| CT-S08.05 | F | Primeira execução (sem ativo), sanidade ok, métrica ≥ `piso`. | Aprovado. | Bloqueado (`piso`) |

### RS-09 — Reprovação, limite K e descarte · `docs/arquitetura.md:714-722`; ADR-011 · C
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-S09.01 | F | Gate reprova. | Versão REPROVADO; execução REPROVADA; checkpoint mantido; ponteiro inalterado. | Condicional |
| CT-S09.02 | F | Execução seguinte a uma REPROVADA. | Treina sobre o modelo **ativo** (não sobre o reprovado), com a janela acumulada desde o mesmo watermark. | Condicional |
| CT-S09.03 | L | Reprovação número `K − 1` / `K`. | Continua tentando / para e alerta. | **Bloqueado:** `K` |
| CT-S09.04 | P | DESCARTADA sem decisão humana auditada. | Nunca ocorre. | Condicional |
| CT-S09.05 | F | Aprovação depois de reprovações. | O contador de reprovações consecutivas zera. | **Bloqueado:** não está escrito que zera (QT-14) |

### RS-10 — Promoção atômica · `docs/arquitetura.md:793`; ADR-010 · B
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-S10.01 | F | Gate aprova. | Ponteiro → candidato; execução CONCLUIDA; checkpoint = corte, na mesma transação. | **Pronto** |
| CT-S10.02 | P | Falha durante a transação final. | Nenhum dos três mudou. | **Pronto** |
| CT-S10.03 | P | Ponteiro trocado com o checkpoint não avançado (ou o contrário). | Nunca ocorre. | **Pronto** |

### RS-11 — Ponteiro e rollback · `docs/arquitetura.md:804,809-820`; ADR-010 · B
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-S11.01 | F | Rollback. | Ponteiro → `versao_anterior_id` (APROVADO_INATIVO); a versão saída vai para APROVADO_INATIVO; ação auditada. | **Pronto** |
| CT-S11.02 | P | Ponteiro apontando para versão CANDIDATO, REPROVADO ou ARQUIVADO. | Nunca ocorre. | **Pronto** |
| CT-S11.03 | P | Alteração do ponteiro por componente que não seja o PromotorDeModelo. | Nunca ocorre (papel `batch_registry`; `api_app` só lê). | **Pronto** |
| CT-S11.04 | C | Rollback sem versão anterior (primeira versão ativa). | — | **Bloqueado:** comportamento indefinido (QT-15) |
| CT-S11.05 | P | Transições proibidas de §6.4: REPROVADO → ATIVO; ARQUIVADO → qualquer estado; CANDIDATO → APROVADO_INATIVO. | Rejeitadas. | **Pronto** (§6.4 lista só as transições permitidas) |

### RS-12 — Versão imutável · `docs/arquitetura.md:802` · B
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-S12.01 | P | Alterar `hash_artefato`, `parametros` ou `execucao_id` de uma VERSAO_MODELO registrada. | Rejeitado. Só o `status` muda, e só pelas transições de §6.4. | **Pronto** |
| CT-S12.02 | P | Sobrescrever um artefato já gravado. | Nunca ocorre (gravação sempre nova, por hash). | Condicional (ADR-013) |

### RS-13 — Leitor do modelo ativo · `docs/arquitetura.md:805`; ADR-013 · C
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-S13.01 | F | O ponteiro muda para uma versão válida. | Depois da recarga, a API usa a nova versão. | Condicional |
| CT-S13.02 | I | O ponteiro muda para um artefato corrompido. | A API mantém a versão atual. | Condicional |
| CT-S13.03 | F | Nenhum modelo ativo disponível. | Fallback sem modelo; a resposta de conteúdo continua 200. | Condicional |
| CT-S13.04 | P | API servindo um modelo meio carregado. | Nunca ocorre. | Condicional |

### RS-14 — Treino incremental · `docs/arquitetura.md:807`; ADR-012 · B
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-S14.01 | F | Execução com modelo ativo V. | Candidato registrado com `base_versao_id` = V. | **Pronto** |
| CT-S14.02 | P | Treino lendo eventos anteriores ao watermark. | Nunca ocorre. | **Pronto** |

### RS-15 — Papel do batch sem acesso a `app` · `docs/arquitetura.md:587-589`; ADR-015 · C
| Caso | Tipo | Cenário | Resultado esperado | Situação |
|------|------|---------|--------------------|----------|
| CT-S15.01 | P | `batch_leitor` tenta ler USUARIO, SESSAO ou ASSINATURA. | Permissão negada. | Condicional |
| CT-S15.02 | F | `batch_leitor` lê `eventos`. | Permitido. | Condicional |
| CT-S15.03 | P | `api_app` tenta escrever em `ml`. | Permissão negada. | Condicional |

---

## 4. Ambiguidades, lacunas e perguntas

### 4.1 Ambiguidades (o artefato diz algo, mas permite mais de uma leitura)

| ID | Ambiguidade | Onde | Casos afetados |
|----|-------------|------|----------------|
| AM-01 | **(Aula 07: resolvida só para `TTL_acesso`, expira no instante exato; §9)** **Bordas temporais:** no instante exato de `fim_vigencia`, `inicio_vigencia`, `TTL_acesso`, `T_rec` e `t_corte`, a regra vale ou não? Nenhum artefato diz se o intervalo é aberto ou fechado. | `docs/arquitetura.md:606,618,622,623` | CT-04.04, CT-06.03, CT-11.03, CT-11.05, CT-13.02 |
| AM-02 | "Taxa de quarentena **acima** do limite": igual ao limite reprova? | `docs/arquitetura.md:799` | CT-S07.03 |
| AM-03 | Os códigos de erro de **login inválido**, cadastro inválido e "anônimo em dados de conta" não estão na matriz. Ela diz só "401", sem código. | `docs/arquitetura.md:607`; ADR-014 | CT-01.02, CT-02.03, CT-05.03, CT-17.02 |
| AM-04 | "Assinante" (`README.md:12`): é só premium vigente, ou também free? O glossário marca como "definição pendente". | `docs/arquitetura.md` §A14 | RN-16 |
| AM-05 | Logout "do dispositivo atual" × logout global. | `docs/arquitetura.md:621`; Q-05 | CT-09.03 |
| AM-06 | Cancelamento corta na hora ou no fim da vigência? | `docs/arquitetura.md:622`; Q-03 | CT-13.01, CT-13.02 |
| AM-07 | O que o free libera além do anônimo. | Q-19 | CT-16.07, CT-19.03 |
| AM-10 | **Como se identifica "dispositivo"?** ADR-004 usa "mesmo dispositivo" na tolerância, e SESSAO tem um campo `dispositivo`, mas nenhum artefato diz de onde vem esse valor nem se o cliente pode forjá-lo. | ADR-004; `docs/arquitetura.md` §3 | CT-05.04, CT-07.04 |

> AM-08 e AM-09 foram reclassificadas como perguntas (QT-07, QT-09), porque o artefato não diz nada a respeito, e não há texto ambíguo.

### 4.2 Lacunas (não há nenhuma regra no artefato)

| ID | Lacuna | Onde procurei | Efeito no TDD |
|----|--------|---------------|---------------|
| LG-01 | Campos, validações e unicidade do **cadastro**. | `README.md:9`; `docs/prd.md`; `docs/arquitetura.md` §3 (só o modelo lógico USUARIO) | RN-01 sem casos prontos |
| LG-02 | Qualquer regra de **perfil**. | `README.md:9`; `docs/arquitetura.md` D6.1 | RN-03 sem casos |
| LG-03 | Regras de **clubes, jogos, resultados e matérias**: placar, status, relações. | `README.md:3,12`; `docs/arquitetura.md` §3 | RN-20 sem casos |
| LG-04 | **Como o conteúdo entra** no sistema (sem painel administrativo). | C-06, Q-01 | Não há como criar dados de conteúdo, só fixtures de teste |
| LG-05 | Mecanismo do **upgrade** sem pagamento real. | C-05, Q-02 | RN-12 bloqueada |
| LG-06 | **Política de senha** e limitação de tentativas de login. | L-06, Q-21 | CT-02.05 |
| LG-07 | Valores de todos os parâmetros da §0.3. | Q-04, Q-15, EXP-10, ADR-011, Q-09, L-05 | Todos os casos L com parâmetro |
| LG-08 | Métricas e conjunto de avaliação do **Modelo M**. | Q-09 | RS-08, RS-04 (chave de negócio) |
| LG-09 | **Metas de cobertura** e critérios de entrada e saída. | `docs/plano-de-teste.md` (todo em `TODO`) | Não há critério de "pronto" para o ciclo |
| LG-10 | **Validade do código de recuperação** e canal de envio. | L-05 | CT-04.04 |

### 4.3 Perguntas novas deste plano

Todas são para o professor ou PO, salvo indicação.

| ID | Pergunta | Casos |
|----|----------|-------|
| QT-01 | O e-mail é único entre as contas? | CT-01.03 |
| QT-02 | Emitir um novo código de recuperação invalida o anterior? | CT-04.07 |
| QT-03 | Novo login no mesmo dispositivo substitui a sessão existente ou cria outra? | CT-05.04 |
| QT-04 | Token expirado **e** revogado: qual código prevalece? **(Aula 07: `token_expirado`, pela ordem da tabela E2; interpretação registrada em §9)** | CT-06.08 |
| QT-05 | O que o logout responde com token já inválido? | CT-09.04 |
| QT-06 | Todo cadastro começa no plano free? | CT-10.01 |
| QT-07 | Um usuário pode ter mais de uma assinatura vigente? | CT-10.03, CT-12.03 |
| QT-08 | Quem valida que `fim_vigencia` > `inicio_vigencia`, e com que erro? | CT-11.07 |
| QT-09 | Um premium vigente pode pedir "upgrade" (renovar ou estender)? | CT-12.02 |
| QT-10 | O que acontece ao cancelar o free ou cancelar duas vezes? | CT-13.05, CT-13.06 |
| QT-11 | Token inválido em rota pública: 401, ou tratar como anônimo? | CT-15.03 |
| QT-12 | Em listas, os itens exclusivos são omitidos ou listados sem corpo para quem não tem acesso? | CT-16.06 |
| QT-13 | Pedido de usuário inexistente: 403 (não revela) ou 404? | CT-18.04 |
| QT-14 | O contador de reprovações consecutivas (K) zera numa aprovação? Arquiteto. | CT-S09.05 |
| QT-15 | Rollback sem versão anterior: proibido, ou volta para "sem modelo"? Arquiteto. | CT-S11.04 |

**Perguntas já abertas em `docs/arquitetura.md` §A12 que bloqueiam casos deste plano:** Q-02, Q-03, Q-04, Q-05, Q-07, Q-09, Q-15, Q-19, Q-21.

---

## 5. Prontidão para o ciclo de TDD

| Situação | Regras | Casos |
|----------|--------|-------|
| **Prontos** (origem A/B, resultado definido) | RN-11, RN-14, RN-15, RN-16, RN-17, RN-18, RN-19; RS-02, RS-03, RS-05, RS-06, RS-10, RS-11, RS-12, RS-14 (e casos isolados de RN-02, RN-10, RN-13, RS-08) | 49 |
| **Condicionais** (ADR Proposta) | RN-04 a RN-09, RN-21; RS-01, RS-04, RS-07, RS-09, RS-13, RS-15 | 53 |
| **Bloqueados** (pergunta ou parâmetro) | RN-01, RN-12; casos L com parâmetro; casos C sem regra | 47 |
| **Sem casos possíveis** | RN-03 (perfil), RN-20 (domínio de conteúdo) | 0 |
| **Total** | 36 regras (21 RN + 15 RS) | **149** |

**Por onde o ciclo pode começar** sem inventar regra (casos prontos, de força B):

1. **RN-16 + RN-19**: matriz de acesso ao conteúdo exclusivo e negação por padrão. Driver D1, o de maior risco (vazamento).
2. **RN-11 + RN-14**: vigência com Relogio fixo e plano fora do token. Os casos L de `fim_vigencia ± 1 ms` estão prontos. O instante exato depende de AM-01.
3. **RN-17 + RN-18**: próprios dados × dados de outro usuário.
4. **RS-02, RS-03, RS-10**: checkpoint, idempotência e promoção atômica (INV-2).

**Antes de abrir os casos condicionais**, confirmar a ADR-004 (sessão), porque ela concentra RN-05 a RN-09.

---

## 6. Fase red: matriz regra → caso → teste → status

> **Execução:** `./mvnw test`, em 2026-11-13, com Docker ativo. Os resultados foram extraídos dos relatórios do Surefire (`target/surefire-reports/`).
> **Framework:** JUnit 5 (Jupiter, com `@ParameterizedTest`) + Testcontainers, como já configurado no `pom.xml`.

### 6.1 Resultado

| Medida | Valor |
|--------|-------|
| Execuções de teste (com parametrizados) | 199 |
| Vermelhas (falha ou erro) | 161, **todas** por `UnsupportedOperationException: não implementado (fase red)` |
| Desabilitadas (`@Disabled`, com o motivo) | 36 |
| Verdes | 2: `contextLoads` (marco zero, gerado pelo Initializr) e CT-10.02 (investigado, §6.3) |
| Casos do plano com teste | **149 de 149** |
| Regras com teste | **36 de 36**, mais um marcador desabilitado para RN-03 e RN-20 |
| Vermelhos por motivo inesperado | **0** (nenhum erro de montagem, de compilação ou de asserção mal escrita) |

**Como cada teste foi verificado:** um script lê os XML do Surefire e confere, para cada falha, se o rastro contém a mensagem da assinatura vazia. Os 161 vermelhos contêm essa mensagem: não há falha "por acaso".
- Quando o teste espera uma exceção de domínio (`FalhaDeAcesso`, `TransicaoProibidaException`, `DescarteNaoPermitidoException`, `CodigoDeRecuperacaoInvalidoException`, `CredenciaisInvalidasException`), a falha é de asserção: veio a exceção errada.
- Nos demais casos, a falha é um erro: a exceção sobe direto.

### 6.2 Regras sem teste executável (destaque)

Toda regra tem pelo menos um teste, mas **cinco não têm nenhum teste que possa ficar verde com implementação**:

| Regra | Situação | Por quê | Para destravar |
|-------|----------|---------|----------------|
| **RN-03** Perfil | Só um marcador desabilitado | Nenhuma regra de perfil em artefato algum (LG-02) | Definir o que é "perfil" |
| **RN-12** Upgrade | 3 casos, todos desabilitados | Mecanismo do upgrade desconhecido (Q-02, S-04); QT-07, QT-09 | Responder Q-02 |
| **RN-20** Domínio de conteúdo | Só um marcador desabilitado | Nenhuma regra sobre clubes, jogos, resultados ou matérias (LG-03) | Levantar as regras de domínio |
| **RN-10** Planos | Nenhum vermelho; 1 verde por construção, 2 desabilitados | Ver §6.3 | QT-06, QT-07 |
| **RN-13** Cancelamento | O caso central (CT-13.01/02) está desabilitado; só os laterais estão vermelhos | `t_corte` é uma regra a decidir, não um parâmetro (Q-03) | Responder Q-03 |

### 6.3 Teste que já passa: investigação

- **CT-10.02 (`AssinaturaTest#rn10_ct10_02_soFreeEPremium`)** passa antes de qualquer implementação.
  - **Causa:** a regra "só existem free e premium" (`README.md:11`) é garantida **pelo tipo**. A assinatura mínima `enum Plano { FREE, PREMIUM }` já a cumpre, e não há lógica a implementar.
  - **Não é caso mal escrito:** o teste falharia se uma terceira constante fosse acrescentada.
  - **Decisão:** manter o teste como guarda de regressão e registrar que esta regra **não tem ciclo red → green**.

### 6.4 Casos desbloqueados nesta fase

O plano marcava como "Bloqueado" 16 casos que agora têm teste vermelho executável:

- **14 casos de valor-limite.** A regra está definida; só o **valor** do parâmetro está pendente. O teste configura um valor arbitrário (`apoio/ValoresDeTeste`, documentado como "não é requisito") e verifica a regra **relativa** a ele, por exemplo "aceita em `TTL − 1 ms`, recusa em `TTL + 1 ms`".
  - Casos: CT-04.04, CT-06.02, CT-06.04, CT-07.02, CT-07.03, CT-07.06, CT-S01.08, CT-S06.03, CT-S07.03, CT-S08.01, CT-S08.02, CT-S08.03, CT-S08.05, CT-S09.03.
  - Continuam desabilitados os casos "**= borda**" (AM-01, AM-02), porque ali o comportamento é que está indefinido, não o valor.
- **2 casos com escopo reduzido:**
  - CT-01.01 verifica só e-mail + senha;
  - CT-02.03 verifica só que a autenticação falha, sem fixar o código de resposta.

### 6.5 Suposições introduzidas pelas assinaturas mínimas

As assinaturas precisam de nomes e formatos para compilar. Elas não têm lógica, mas **fixam contratos** que a fase green herdará. Todos derivam de artefatos; os que não derivam estão marcados.

| Contrato | Origem | Observação |
|----------|--------|------------|
| Componentes e nomes (`PoliticaDeAcesso`, `ServicoDeSessao`, `OrquestradorDoJob`, `PromotorDeModelo`, `LeitorDoModeloAtivo`…) | Catálogo `docs/arquitetura.md` §2.1 | Nomes canônicos |
| Códigos de erro (`CodigoDeErro`) | §4.1, §4.2 | Os nomes são hipótese de contrato (ADR-014, Q-07). O teste não fixa o nome da propriedade do Problem Details. |
| Login por e-mail + senha | §3 (USUARIO tem `email`, `hash_senha`); §4.2 (recuperação por e-mail) | Derivado, não explícito |
| Dispositivo informado pelo cliente como texto | ADR-004 | **AM-10 continua aberta**: o teste de CT-07.04 só funciona se o dispositivo não puder ser forjado |
| Métrica do gate como pontuação "maior é melhor" | §6.3 item 12 ("≥ ativo − tolerância") | A métrica real é Q-09 |
| `Treinador`, `AvaliadorDeCandidato` e `CarregadorDeModelo` como portas substituídas por dublês | ADR-012, ADR-013 | Algoritmo e métricas pendentes (Q-09) |
| Nomes de tabela em minúsculas a partir das entidades de §3 (`ml.ponteiro_modelo_ativo`, `app.usuario`, `eventos.evento_de_entrada`) | §3 | Usados só nos testes de papéis (RS-15, CT-S11.03) |
| Rollback "auditado" | §6.3 item 13 | **Não verificável:** não existe componente de auditoria (DIV-03). CT-S11.01 verifica só ponteiro e estados. |
| "Resposta continua 200" no fallback sem modelo | §6.3 item 14 | **Não verificável:** o uso do Modelo M pela API é Q-09. CT-S13.03 verifica só que não há erro e que nenhum modelo é servido. |

### 6.6 Matriz (fase red; registro histórico, a matriz atual está na §7.5)

"Situação no plano" é a coluna da §2/§3. "Status red" é o resultado real da execução.

| Regra | Caso | Teste (classe#método) | Situação no plano | Status red | Observação |
|-------|------|-----------------------|-------------------|------------|------------|
| RN-01 | CT-01.01 | `IdentidadeTest#rn01_ct01_01_cadastroPermiteAutenticar` | Bloqueado: quais campos são obrigatórios e o que é "válido" não está definido (LG-01) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · **desbloqueado parcialmente**: verifica só e-mail + senha; os demais campos seguem LG-01 |
| RN-01 | CT-01.02 | `IdentidadeTest#rn01_ct01_02_campoObrigatorioAusente` | Bloqueado: campos e código de erro indefinidos (LG-01, AM-03) | **DESABILITADO** | Bloqueado: LG-01, campos obrigatórios e código de erro do cadastro não definidos |
| RN-01 | CT-01.03 | `IdentidadeTest#rn01_ct01_03_emailDuplicado` | Bloqueado: a unicidade do e-mail não está escrita em lugar nenhum (QT-01) | **DESABILITADO** | Bloqueado: QT-01, unicidade do e-mail não escrita em nenhum artefato |
| RN-01 | CT-01.04 | `IdentidadeTest#rn01_ct01_04_contaSemCredencial` | Bloqueado: a regra só existe implicitamente (LG-01) | **DESABILITADO** | Bloqueado: LG-01, regra de conta sem credencial só implícita |
| RN-02 | CT-02.01 | `IdentidadeTest#rn02_ct02_01_credencialArmazenadaComoHash` | Pronto (mecanismo) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-02 | CT-02.02 | `IdentidadeTest#rn02_ct02_02_senhaCorretaAutentica` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-02 | CT-02.03 | `IdentidadeTest#rn02_ct02_03_senhaErradaNaoAutentica` | Bloqueado: código e corpo da resposta de falha de login indefinidos (AM-03) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · **desbloqueado parcialmente**: verifica só que não autentica; o código de resposta segue AM-03 |
| RN-02 | CT-02.04 | `IdentidadeTest#rn02_ct02_04_senhaNuncaEmLog` | Condicional (ADR-015: "nenhum token, senha ou e-mail em log") | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-02 | CT-02.05 | `IdentidadeTest#rn02_ct02_05_senhaForaDaPolitica` | Bloqueado: sem política de credencial (L-06) | **DESABILITADO** | Bloqueado: L-06, sem política de credencial |
| RN-03 | (sem caso) | `IdentidadeTest#rn03_semCasoPossivel` | — | **DESABILITADO** | Sem regra: LG-02, o artefato só diz que o perfil existe (docs/arquitetura.md D6.1) |
| RN-04 | CT-04.01 | `IdentidadeTest#rn04_ct04_01_recuperaERevogaSessoes` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-04 | CT-04.02 | `IdentidadeTest#rn04_ct04_02_respostaIdenticaParaEmailInexistente` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-04 | CT-04.03 | `IdentidadeTest#rn04_ct04_03_codigoDeUsoUnico` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-04 | CT-04.04 | `IdentidadeTest#rn04_ct04_04a_codigoAceitoAntesDaValidade`<br>`IdentidadeTest#rn04_ct04_04b_codigoRejeitadoAposValidade`<br>`IdentidadeTest#rn04_ct04_04c_codigoNaBordaDaValidade` | Bloqueado: `T_rec` (L-05) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · 3 execuções · **desbloqueado**: teste relativo ao parâmetro configurado (valor real segue pendente) |
| RN-04 | CT-04.05 | `IdentidadeTest#rn04_ct04_05_codigoAdulteradoNaoRevoga` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-04 | CT-04.06 | `IdentidadeTest#rn04_ct04_06_codigoGuardadoSoComoHash` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-04 | CT-04.07 | `IdentidadeTest#rn04_ct04_07_doisCodigosEmSequencia` | Bloqueado: não se sabe se o novo invalida o anterior (QT-02) | **DESABILITADO** | Bloqueado: QT-02, novo código invalida o anterior? |
| RN-05 | CT-05.01 | `SessaoTest#rn05_ct05_01_loginCriaSessaoEEmitePar` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-05 | CT-05.02 | `SessaoTest#rn05_ct05_02_doisDispositivosDuasSessoes` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-05 | CT-05.03 | `SessaoTest#rn05_ct05_03_credencialInvalidaNaoCriaSessao` | Condicional (o código de resposta é AM-03) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-05 | CT-05.04 | `SessaoTest#rn05_ct05_04_novoLoginMesmoDispositivo` | Bloqueado: não se sabe se substitui ou cria outra sessão (QT-03, AM-10) | **DESABILITADO** | Bloqueado: QT-03 / AM-10, novo login no mesmo dispositivo substitui ou cria sessão? |
| RN-05 | CT-05.05 | `SessaoTest#rn05_ct05_05_limiteDeSessoes` | Bloqueado: limite de sessões indefinido (Q-05) | **DESABILITADO** | Bloqueado: Q-05, limite de sessões não definido |
| RN-06 | CT-06.01 | `SessaoTest#rn06_ct06_01_tokenValidoIdentificaUsuario` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-06 | CT-06.02 | `SessaoTest#rn06_ct06_02_aceitoAntesDoTtl` | Bloqueado: `TTL_acesso` (Q-04) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · **desbloqueado**: teste relativo ao parâmetro configurado (valor real segue pendente) |
| RN-06 | CT-06.03 | `SessaoTest#rn06_ct06_03_bordaDoTtl` | Bloqueado | **DESABILITADO** | Bloqueado: AM-01, no instante exato do TTL o token expira? |
| RN-06 | CT-06.04 | `SessaoTest#rn06_ct06_04_expiradoAposTtl` | Bloqueado: `TTL_acesso` | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · **desbloqueado**: teste relativo ao parâmetro configurado (valor real segue pendente) |
| RN-06 | CT-06.05 | `SessaoTest#rn06_ct06_05_tokenMalformado` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-06 | CT-06.06 | `SessaoTest#rn06_ct06_06_tokenInexistente` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-06 | CT-06.07 | `SessaoTest#rn06_ct06_07_tokenDeSessaoRevogada` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-06 | CT-06.08 | `SessaoTest#rn06_ct06_08_expiradoERevogado` | Bloqueado: qual código prevalece (QT-04) | **DESABILITADO** | Bloqueado: QT-04, expirado e revogado ao mesmo tempo: qual código prevalece? |
| RN-07 | CT-07.01 | `SessaoTest#rn07_ct07_01_renovacaoRotaciona` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-07 | CT-07.02 | `SessaoTest#rn07_ct07_02_reusoDentroDaTolerancia` | Bloqueado: `T_tol` (EXP-10) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · **desbloqueado**: teste relativo ao parâmetro configurado (valor real segue pendente) |
| RN-07 | CT-07.03 | `SessaoTest#rn07_ct07_03_reusoForaDaToleranciaRevoga` | Bloqueado: `T_tol` | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · **desbloqueado**: teste relativo ao parâmetro configurado (valor real segue pendente) |
| RN-07 | CT-07.04 | `SessaoTest#rn07_ct07_04_reusoDeOutroDispositivoRevoga` | Condicional; AM-10 (como se identifica "dispositivo") | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-07 | CT-07.05 | `SessaoTest#rn07_ct07_05_refreshMaisAntigoRevoga` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-07 | CT-07.06 | `SessaoTest#rn07_ct07_06a_renovacaoAntesDoTtlRefresh`<br>`SessaoTest#rn07_ct07_06b_renovacaoAposTtlRefresh` | Bloqueado: `TTL_refresh` (Q-04) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · 2 execuções · **desbloqueado**: teste relativo ao parâmetro configurado (valor real segue pendente) |
| RN-07 | CT-07.07 | `SessaoTest#rn07_ct07_07_refreshInvalido` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-07 | CT-07.08 | `SessaoTest#rn07_ct07_08_nuncaDoisParesValidos` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-08 | CT-08.01 | `SessaoTest#rn08_ct08_01_corridaNaMesmaSessao` | Condicional (depende de `T_tol`) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-08 | CT-08.02 | `SessaoTest#rn08_ct08_02_corridaEmDispositivosDiferentes` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-08 | CT-08.03 | `SessaoTest#rn08_ct08_03_umUnicoParAposCorrida` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-09 | CT-09.01 | `SessaoTest#rn09_ct09_01_logoutInvalidaTokenNaHora` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-09 | CT-09.02 | `SessaoTest#rn09_ct09_02_logoutInvalidaRefresh` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-09 | CT-09.03 | `SessaoTest#rn09_ct09_03_logoutSoDoDispositivoAtual` | Condicional; Q-05 (logout global) pode mudar | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-09 | CT-09.04 | `SessaoTest#rn09_ct09_04_logoutComTokenInvalido` | Bloqueado: resposta indefinida (QT-05) | **DESABILITADO** | Bloqueado: QT-05, resposta do logout com token já inválido não definida |
| RN-09 | CT-09.05 | `SessaoTest#rn09_ct09_05_sessaoRevogadaNaoRessuscita` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-10 | CT-10.01 | `AssinaturaTest#rn10_ct10_01_planoInicial` | Bloqueado: não se sabe se todo cadastro começa no free (QT-06) | **DESABILITADO** | Bloqueado: QT-06, todo cadastro começa no free? |
| RN-10 | CT-10.02 | `AssinaturaTest#rn10_ct10_02_soFreeEPremium` | Pronto (só existem esses dois: `README.md:11`) | **VERDE (investigado)** | Passa por construção: só existem as constantes FREE e PREMIUM no enum Plano (assinatura mínima de README.md:11). Não há lógica a implementar; fica como guarda de regressão. |
| RN-10 | CT-10.03 | `AssinaturaTest#rn10_ct10_03_doisPlanosVigentes` | Bloqueado: não está escrito (QT-07) | **DESABILITADO** | Bloqueado: QT-07, mais de uma assinatura vigente é permitida? |
| RN-11 | CT-11.01 | `AssinaturaTest#rn11_ct11_01_dentroDaVigencia` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-11 | CT-11.02 | `AssinaturaTest#rn11_ct11_02_antesDoInicio` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-11 | CT-11.03 | `AssinaturaTest#rn11_ct11_03_bordaDoInicio` | Bloqueado | **DESABILITADO** | Bloqueado: AM-01, no instante exato do início a vigência vale? |
| RN-11 | CT-11.04 | `AssinaturaTest#rn11_ct11_04_antesDoFim` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-11 | CT-11.05 | `AssinaturaTest#rn11_ct11_05_bordaDoFim` | Bloqueado | **DESABILITADO** | Bloqueado: AM-01, no instante exato do fim a vigência vale? |
| RN-11 | CT-11.06 | `AssinaturaTest#rn11_ct11_06_aposOFim` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-11 | CT-11.07 | `AssinaturaTest#rn11_ct11_07_fimAntesDoInicio` | Bloqueado: a validação não está escrita (QT-08) | **DESABILITADO** | Bloqueado: QT-08, validação de fim < início não escrita |
| RN-12 | CT-12.01 | `AssinaturaTest#rn12_ct12_01_upgradeLiberaExclusivo` | Bloqueado: o mecanismo do upgrade é desconhecido (Q-02; S-04) | **DESABILITADO** | Bloqueado: Q-02 / S-04, mecanismo do upgrade sem pagamento real desconhecido |
| RN-12 | CT-12.02 | `AssinaturaTest#rn12_ct12_02_upgradeDePremium` | Bloqueado (QT-09) | **DESABILITADO** | Bloqueado: QT-09, premium vigente pode pedir upgrade? |
| RN-12 | CT-12.03 | `AssinaturaTest#rn12_ct12_03_upgradeDuplicado` | Bloqueado (QT-07) | **DESABILITADO** | Bloqueado: QT-07, upgrade pode gerar duas assinaturas vigentes? |
| RN-13 | CT-13.01 | `AssinaturaTest#rn13_ct13_01_exclusivoNegadoAposCorte` | Bloqueado: `t_corte` (Q-03: imediato ou fim da vigência?) | **DESABILITADO** | Bloqueado: Q-03, cancelamento corta na hora ou no fim da vigência (t_corte)? |
| RN-13 | CT-13.02 | `AssinaturaTest#rn13_ct13_02_bordaDoCorte` | Bloqueado: Q-03; AM-01 para `= t_corte` | **DESABILITADO** | Bloqueado: Q-03 (t_corte) e AM-01 (borda) |
| RN-13 | CT-13.03 | `AssinaturaTest#rn13_ct13_03_publicoAposCancelar` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-13 | CT-13.04 | `AssinaturaTest#rn13_ct13_04_contaESessaoAposCancelar` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-13 | CT-13.05 | `AssinaturaTest#rn13_ct13_05_cancelarFree` | Bloqueado (QT-10) | **DESABILITADO** | Bloqueado: QT-10, cancelar o plano free |
| RN-13 | CT-13.06 | `AssinaturaTest#rn13_ct13_06_cancelarDuasVezes` | Bloqueado (QT-10) | **DESABILITADO** | Bloqueado: QT-10, cancelar duas vezes |
| RN-14 | CT-14.01 | `AssinaturaTest#rn14_ct14_01_tokenNaoCarregaPrivilegio` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-14 | CT-14.02 | `AssinaturaTest#rn14_ct14_02_upgradeSemNovoLogin` | Bloqueado pelo mecanismo de upgrade (Q-02) | **DESABILITADO** | Bloqueado: Q-02, mecanismo do upgrade |
| RN-15 | CT-15.01 | `AcessoAoConteudoTest#rn15_ct15_01_anonimoLePublico` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-15 | CT-15.02 | `AcessoAoConteudoTest#rn15_ct15_02_todosOsPerfisLeemPublico` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · 3 execuções |
| RN-15 | CT-15.03 | `AcessoAoConteudoTest#rn15_ct15_03_tokenInvalidoEmPublico` | Bloqueado: 401 ou tratar como anônimo? (QT-11) | **DESABILITADO** | Bloqueado: QT-11, token inválido em rota pública dá 401 ou é tratado como anônimo? |
| RN-16 | CT-16.01 | `AcessoAoConteudoTest#rn16_ct16_01_premiumVigenteLeExclusivo` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-16 | CT-16.02 | `AcessoAoConteudoTest#rn16_ct16_02_anonimoEmExclusivo` | Pronto (o nome do código é condicional, ADR-014) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-16 | CT-16.03 | `AcessoAoConteudoTest#rn16_ct16_03_freeEmExclusivo` | Pronto (o nome do código é condicional) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-16 | CT-16.04 | `AcessoAoConteudoTest#rn16_ct16_04_premiumVencidoEmExclusivo` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-16 | CT-16.05 | `AcessoAoConteudoTest#rn16_ct16_05_negacaoSemConteudo` | Pronto (CEN-01: "sem nenhum campo do conteúdo exclusivo no corpo") | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-16 | CT-16.06 | `AcessoAoConteudoTest#rn16_ct16_06_listaMista` | Bloqueado: não se sabe se eles são omitidos ou listados sem corpo (QT-12) | **DESABILITADO** | Bloqueado: QT-12, em listas os exclusivos são omitidos ou listados sem corpo? |
| RN-16 | CT-16.07 | `AcessoAoConteudoTest#rn16_ct16_07_freeAlemDoAnonimo` | Bloqueado (Q-19) | **DESABILITADO** | Bloqueado: Q-19, o que o free vê a mais que o anônimo |
| RN-17 | CT-17.01 | `DadosDeUsuarioTest#rn17_ct17_01_propriosDadosPermitidos` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-17 | CT-17.02 | `DadosDeUsuarioTest#rn17_ct17_02_anonimoEmDadosDeConta` | Pronto (o código específico não está definido na matriz: AM-03) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-17 | CT-17.03 | `DadosDeUsuarioTest#rn17_ct17_03_premiumVencidoPropriosDados` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-18 | CT-18.01 | `DadosDeUsuarioTest#rn18_ct18_01_freeEmOutroUsuario` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-18 | CT-18.02 | `DadosDeUsuarioTest#rn18_ct18_02_premiumEmOutroUsuario` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-18 | CT-18.03 | `DadosDeUsuarioTest#rn18_ct18_03_anonimoEmOutroUsuario` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-18 | CT-18.04 | `DadosDeUsuarioTest#rn18_ct18_04_usuarioInexistente` | Bloqueado: 403 ou 404? O 404 revelaria a existência de contas (QT-13) | **DESABILITADO** | Bloqueado: QT-13, usuário inexistente: 403 ou 404? |
| RN-19 | CT-19.01 | `AcessoAoConteudoTest#rn19_ct19_01_semClassificacaoNegado` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-19 | CT-19.02 | `AcessoAoConteudoTest#rn19_ct19_02_todoCaminhoPassaPelaPolitica` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-19 | CT-19.03 | `AcessoAoConteudoTest#rn19_ct19_03_matrizCompleta` | Pronto, exceto as células do free (Q-19) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · 8 execuções |
| RN-20 | (sem caso) | `AcessoAoConteudoTest#rn20_semCasoPossivel` | — | **DESABILITADO** | Sem regra: LG-03, nenhum artefato define regra sobre clubes, jogos, resultados ou matérias |
| RN-21 | CT-21.01 | `ContratoDeErroTest#rn21_ct21_01_problemDetailsComCodigoEstavel` | Condicional (Q-07) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · 5 execuções |
| RN-21 | CT-21.02 | `ContratoDeErroTest#rn21_ct21_02_mesmaSituacaoMesmoCodigo` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RN-21 | CT-21.03 | `ContratoDeErroTest#rn21_ct21_03_semDetalheInterno` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-01 | CT-S01.01 | `JanelaIncrementalTest#rs01_cts01_01_eventosDentroDaJanela` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-01 | CT-S01.02 | `JanelaIncrementalTest#rs01_cts01_02_noWatermarkFora` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-01 | CT-S01.03 | `JanelaIncrementalTest#rs01_cts01_03_logoAposWatermarkDentro` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-01 | CT-S01.04 | `JanelaIncrementalTest#rs01_cts01_04_noCorteDentro` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-01 | CT-S01.05 | `JanelaIncrementalTest#rs01_cts01_05_aposCorteFora` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-01 | CT-S01.06 | `JanelaIncrementalTest#rs01_cts01_06_eventoTardioEntra` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-01 | CT-S01.07 | `OrquestradorDoJobTest#rs01_cts01_07_eventoConsumidoNaoReaparece` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-01 | CT-S01.08 | `OrquestradorDoJobTest#rs01_cts01_08_corteEInicioMenosMargem` | Bloqueado: `Δ` (Q-15) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · **desbloqueado**: teste relativo ao parâmetro configurado (valor real segue pendente) |
| RS-02 | CT-S02.01 | `OrquestradorDoJobTest#rs02_cts02_01_concluidaAvancaCheckpoint` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-02 | CT-S02.02 | `OrquestradorDoJobTest#rs02_cts02_02_falhaMantemCheckpoint` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-02 | CT-S02.03 | `OrquestradorDoJobTest#rs02_cts02_03_reprovadaMantemCheckpoint` | Condicional (ADR-011) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-02 | CT-S02.04 | `OrquestradorDoJobTest#rs02_cts02_04_descarteAvancaCheckpoint` | Condicional (ADR-011) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-02 | CT-S02.05 | `OrquestradorDoJobTest#rs02_cts02_05_checkpointSoDeExecucaoTerminal` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-03 | CT-S03.01 | `OrquestradorDoJobTest#rs03_cts03_01_concluidaSemEfeito` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-03 | CT-S03.02 | `OrquestradorDoJobTest#rs03_cts03_02_retomadaComMesmaJanela` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-03 | CT-S03.03 | `PersistenciaDoTreinoIntegracaoTest#rs03_cts03_03_dataReferenciaUnica` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-04 | CT-S04.01 | `JanelaIncrementalTest#rs04_cts04_01_registradorNaoDuplica` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-04 | CT-S04.02 | `JanelaIncrementalTest#rs04_cts04_02_extratorDeduplica` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-04 | CT-S04.03 | `JanelaIncrementalTest#rs04_cts04_03_duplicataDeNegocio` | Bloqueado: chave de negócio (Q-09; RA-02) | **DESABILITADO** | Bloqueado: Q-09 / RA-02, chave de negócio de deduplicação depende do Modelo M |
| RS-05 | CT-S05.01 | `OrquestradorDoJobTest#rs05_cts05_01_lockIndisponivelSemEfeito` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-05 | CT-S05.02 | `PersistenciaDoTreinoIntegracaoTest#rs05_cts05_02_quedaLiberaLock` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-05 | CT-S05.03 | `PersistenciaDoTreinoIntegracaoTest#rs05_cts05_03_umLockPorVez` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-06 | CT-S06.01 | `OrquestradorDoJobTest#rs06_cts06_01_falhaAposCandidato` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-06 | CT-S06.02 | `OrquestradorDoJobTest#rs06_cts06_02_orfaoArquivadoNaSeguinte` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-06 | CT-S06.03 | `OrquestradorDoJobTest#rs06_cts06_03a_transitorioRecupera`<br>`OrquestradorDoJobTest#rs06_cts06_03b_transitorioEsgota` | Bloqueado: número de tentativas indefinido | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · 2 execuções · **desbloqueado**: teste relativo ao parâmetro configurado (valor real segue pendente) |
| RS-06 | CT-S06.04 | `OrquestradorDoJobTest#rs06_cts06_04_erroDeDadosSemRetry` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-07 | CT-S07.01 | `ValidadorDeDadosTest#rs07_cts07_01_esquemasNeNMenos1` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-07 | CT-S07.02 | `ValidadorDeDadosTest#rs07_cts07_02_esquemaDesconhecidoEmQuarentena` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-07 | CT-S07.03 | `ValidadorDeDadosTest#rs07_cts07_03c_igualAoLimite`<br>`ValidadorDeDadosTest#rs07_cts07_03b_acimaDoLimite`<br>`ValidadorDeDadosTest#rs07_cts07_03a_abaixoDoLimite` | Bloqueado: `L_quar` | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · 3 execuções · **desbloqueado**: teste relativo ao parâmetro configurado (valor real segue pendente) |
| RS-07 | CT-S07.04 | `OrquestradorDoJobTest#rs07_cts07_04_treinoSemEsquemaDesconhecido` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-07 | CT-S07.05 | `ValidadorDeDadosTest#rs07_cts07_05_volumeMinimo` | Bloqueado: volume mínimo indefinido | **DESABILITADO** | Bloqueado: volume mínimo da janela não definido (§6.3 item 8) |
| RS-08 | CT-S08.01 | `GateDePromocaoTest#rs08_cts08_01_candidatoMelhorAprova` | Bloqueado (métrica, Q-09) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · **desbloqueado**: teste relativo ao parâmetro configurado (valor real segue pendente) |
| RS-08 | CT-S08.02 | `GateDePromocaoTest#rs08_cts08_02_exatamenteNaToleranciaAprova` | Bloqueado (`tol_gate`) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · **desbloqueado**: teste relativo ao parâmetro configurado (valor real segue pendente) |
| RS-08 | CT-S08.03 | `GateDePromocaoTest#rs08_cts08_03_abaixoDaToleranciaReprova` | Bloqueado | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · **desbloqueado**: teste relativo ao parâmetro configurado (valor real segue pendente) |
| RS-08 | CT-S08.04 | `GateDePromocaoTest#rs08_cts08_04_sanidadeEDadosSaoObrigatorios` | Pronto (a sanidade é condição obrigatória) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-08 | CT-S08.05 | `GateDePromocaoTest#rs08_cts08_05b_primeiraExecucaoAbaixoDoPiso`<br>`GateDePromocaoTest#rs08_cts08_05a_primeiraExecucaoNoPiso` | Bloqueado (`piso`) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · 2 execuções · **desbloqueado**: teste relativo ao parâmetro configurado (valor real segue pendente) |
| RS-09 | CT-S09.01 | `OrquestradorDoJobTest#rs09_cts09_01_reprovacao` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-09 | CT-S09.02 | `OrquestradorDoJobTest#rs09_cts09_02_proximaUsaAtivoEJanelaAcumulada` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-09 | CT-S09.03 | `OrquestradorDoJobTest#rs09_cts09_03_limiteK` | Bloqueado: `K` | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · **desbloqueado**: teste relativo ao parâmetro configurado (valor real segue pendente) |
| RS-09 | CT-S09.04 | `OrquestradorDoJobTest#rs09_cts09_04a_descarteAntesDeK`<br>`OrquestradorDoJobTest#rs09_cts09_04b_descarteSemResponsavel` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · 2 execuções |
| RS-09 | CT-S09.05 | `OrquestradorDoJobTest#rs09_cts09_05_contadorZera` | Bloqueado: não está escrito que zera (QT-14) | **DESABILITADO** | Bloqueado: QT-14, o contador de reprovações consecutivas zera numa aprovação? |
| RS-10 | CT-S10.01 | `OrquestradorDoJobTest#rs10_cts10_01_promocao` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-10 | CT-S10.02 | `PersistenciaDoTreinoIntegracaoTest#rs10_cts10_02_transacaoFinalTudoOuNada` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-10 | CT-S10.03 | `OrquestradorDoJobTest#rs10_cts10_03_ponteiroECheckpointJuntos` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-11 | CT-S11.01 | `VersoesEPonteiroTest#rs11_cts11_01_rollback` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-11 | CT-S11.02 | `VersoesEPonteiroTest#rs11_cts11_02_ponteiroSoParaVersaoAprovada` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · 3 execuções |
| RS-11 | CT-S11.03 | `PersistenciaDoTreinoIntegracaoTest#rs11_cts11_03_apiNaoAlteraPonteiro` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-11 | CT-S11.04 | `VersoesEPonteiroTest#rs11_cts11_04_rollbackSemAnterior` | Bloqueado: comportamento indefinido (QT-15) | **DESABILITADO** | Bloqueado: QT-15, rollback sem versão anterior é proibido ou volta para sem modelo? |
| RS-11 | CT-S11.05 | `VersoesEPonteiroTest#rs11_cts11_05_transicoesDeVersao` | Pronto (§6.4 lista só as transições permitidas) | **VERMELHO** | falha por UnsupportedOperationException (não implementado) · 25 execuções |
| RS-12 | CT-S12.01 | `PersistenciaDoTreinoIntegracaoTest#rs12_cts12_01_versaoImutavel` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-12 | CT-S12.02 | `PersistenciaDoTreinoIntegracaoTest#rs12_cts12_02_artefatoNaoSobrescrito` | Condicional (ADR-013) | **DESABILITADO** | Bloqueado: L-11, tecnologia do Armazenamento de Artefatos adiada (ADR-013) |
| RS-13 | CT-S13.01 | `LeitorDoModeloAtivoTest#rs13_cts13_01_trocaAposCargaValida` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-13 | CT-S13.02 | `LeitorDoModeloAtivoTest#rs13_cts13_02_corrompidoMantemAtual` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-13 | CT-S13.03 | `LeitorDoModeloAtivoTest#rs13_cts13_03_fallbackSemModelo` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-13 | CT-S13.04 | `LeitorDoModeloAtivoTest#rs13_cts13_04_artefatoAusenteMantemAtual` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-14 | CT-S14.01 | `OrquestradorDoJobTest#rs14_cts14_01_baseEOAtivo` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-14 | CT-S14.02 | `OrquestradorDoJobTest#rs14_cts14_02_semDadosAntigos` | Pronto | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-15 | CT-S15.01 | `PersistenciaDoTreinoIntegracaoTest#rs15_cts15_01_batchLeitorSemAcessoAApp` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-15 | CT-S15.02 | `PersistenciaDoTreinoIntegracaoTest#rs15_cts15_02_batchLeitorLeEventos` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |
| RS-15 | CT-S15.03 | `PersistenciaDoTreinoIntegracaoTest#rs15_cts15_03_apiNaoEscreveEmMl` | Condicional | **VERMELHO** | falha por UnsupportedOperationException (não implementado) |

---

## 7. Fase green: implementação e matriz atual

> **Execução:** `./mvnw -B verify`, em 2026-11-13: **BUILD SUCCESS**, com 199 execuções, 0 falhas, 0 erros e 36 desabilitadas. Os resultados foram extraídos dos relatórios do Surefire. Os testes de integração também passaram 3 vezes seguidas em **ordem aleatória** (`MethodOrderer$Random`).

### 7.1 Resultado

| Medida | Fase red (§6) | Fase green |
|--------|---------------|------------|
| Verdes | 2 | **163** |
| Vermelhos | 161 | **0** |
| Desabilitados | 36 | 36 (os mesmos, nenhum novo `@Disabled`) |
| Casos do plano com teste verde | 1 | **117** (os 32 casos restantes seguem desabilitados por pergunta ou lacuna) |

**Testes intactos:** em relação ao commit da fase red (`67c0db3`), nenhuma asserção, `@Disabled`, `@Test` ou resultado esperado mudou. O único arquivo de teste alterado é `PersistenciaDoTreinoIntegracaoTest`, e só na preparação (§7.3).

### 7.2 Como foi feito

O trabalho seguiu **um componente por vez**, com a suíte inteira rodada depois de cada passo:

| Passo | Componentes (nomes do catálogo §2.1) | ADRs | Suíte depois do passo |
|-------|--------------------------------------|------|-----------------------|
| 1 | TratadorDeErros, ServicoDeAssinatura, PoliticaDeAcesso, ServicoDeConteudo | 002, 003, 005, 014 | 39 verdes; o restante falhava só por "não implementado" |
| 2 | ServicoDeIdentidade, ServicoDeSessao (+ `seguranca/Hashes`, funções de apoio que não são componente) | 004, 015 | 78 verdes |
| 3 | ExtratorIncremental, RegistradorDeEventos, ValidadorDeDados, GateDePromocao, TransicoesDeVersao, PromotorDeModelo, LeitorDoModeloAtivo, OrquestradorDoJob | 007–013 | 154 verdes; faltavam só os 9 de integração |
| 4 | ProvisionadorDeBanco, RegistryDeModelosJdbc, GerenciadorDeLockPostgres | 009, 010, 015 | 161 verdes e 2 erros de isolamento (§7.3) |
| 5 | Isolamento da classe de integração (aprovado pelo usuário) | — | **163 verdes** |

O passo de refatoração com a suíte verde não encontrou duplicação nem complexidade que justificasse mudança.

**Pontos da sessão (ADR-004):**
- Os tokens são opacos (`idDaSessao.versao.assinatura`), assinados com HMAC por uma chave do servidor.
- A SESSAO guarda só hashes.
- O "mesmo par" da tolerância de reuso é recalculado a partir da versão, sem guardar texto puro.
- A rotação e a revogação usam controle otimista: a revogação também incrementa a versão, para não se perder numa corrida.

### 7.3 Divergência encontrada num teste e como foi resolvida

| Item | Conteúdo |
|------|----------|
| Teste | `PersistenciaDoTreinoIntegracaoTest` (CT-S03.03, CT-S12.01; o mesmo risco afetava CT-S05.02/03) |
| Sintoma | Erro no **Arrange**: `duplicate key ... execucao_treino_data_referencia_key` |
| Causa | Um único PostgreSQL compartilhado pela classe (`static @Container`), sem limpeza entre testes. Três testes inserem execução na mesma data, e o resultado dependia da ordem de execução. O CT-S05.03 deixava o advisory lock preso. |
| Confronto com a fonte | A regra RS-03 ("data_referencia única", §6.3 item 3; ADR-009) estava **correta** e funcionando. O defeito era de isolamento do teste, não de expectativa. |
| Decisão | Perguntada ao usuário antes de mudar. Opção escolhida: limpar no `@BeforeEach` (TRUNCATE do schema `ml`, checkpoint de volta a `epoch`, encerrar conexões que restaram de testes anteriores). |
| O que **não** mudou | Asserções, resultados esperados, `@Disabled`, dados de entrada dos casos |

### 7.4 Decisões provisórias tomadas na implementação

Nenhum teste fixa estas escolhas. Elas existem porque o código precisa de *algum* comportamento, e cada uma aponta para a pergunta que a decide.

| Onde | Comportamento implementado | Pendência |
|------|----------------------------|-----------|
| ServicoDeAssinatura: bordas da vigência | O início conta como vigente; o fim não | AM-01 |
| ServicoDeAssinatura: cancelamento | Registra `canceladaEm`, mas **não corta o acesso antes de `fimVigencia`** | **Q-03**: se o corte for imediato, a regra muda aqui |
| ServicoDeAssinatura: upgrade | Lança "pendente Q-02". Nenhum teste o exercita (RN-12 inteira está desabilitada). | Q-02 |
| ServicoDeAssinatura: cancelar sem premium | Não faz nada | QT-10 |
| ServicoDeConteudo: listagem | Omite os itens negados | QT-12 |
| ServicoDeConteudo: matéria inexistente | `NoSuchElementException` | Sem regra (404 × 403, ver QT-13) |
| ServicoDeIdentidade: cadastro | Só e-mail e senha; não impede e-mail repetido | LG-01, QT-01 |
| ServicoDeIdentidade: código de recuperação | Válido até `emitidoEm + validade`, inclusive o instante exato | AM-01, L-05 |
| ServicoDeSessao: bordas de TTL e tolerância | ~~O instante exato ainda vale~~ **(Aula 07)** Token de acesso: expira no instante exato (decidido, §9). Refresh e tolerância de reuso: o instante exato ainda vale (provisório) | AM-01 |
| ServicoDeSessao: logout com token inválido | `token_invalido` | QT-05 |
| ServicoDeSessao: expiração do refresh | Renovada a cada rotação (janela deslizante) | Q-04 |
| ServicoDeSessao: chave HMAC | Gerada por instância. **Em produção deve vir de configuração secreta**, senão um reinício invalida as sessões. | Infraestrutura (L-11) |
| ValidadorDeDados: taxa igual ao limite | Aprova (lê "acima" literalmente) | AM-02 |
| ValidadorDeDados: janela vazia | Aprova | Volume mínimo (§6.3 item 8) |
| OrquestradorDoJob: contagem de reprovações | Zera na última CONCLUIDA ou DESCARTADA | QT-14 |
| OrquestradorDoJob: descarte | Só a REPROVADA mais recente, com responsável e depois de K reprovações | ADR-011 |
| PromotorDeModelo: rollback sem anterior | Proibido (`TransicaoProibidaException`) | QT-15 |
| PromotorDeModelo: auditoria | Não registrada: não existe componente de auditoria | DIV-03 |

**Riscos residuais da implementação:**
1. **Status das versões fora da transação final.** No adaptador JDBC, a mudança de status das versões (ATIVO / APROVADO_INATIVO) acontece antes de `finalizar` e fora da transação dele. Ponteiro, execução e checkpoint continuam atômicos (CT-S10.02), mas uma queda entre os passos pode deixar o status de uma versão divergente do ponteiro. Para fechar, o status precisa entrar na transação de `finalizar`.
2. **Custo do RegistradorDeEventos.** Ele verifica duplicata lendo todos os eventos, o que é linear no volume. No banco real, a unicidade de `id_evento` já é garantida pela restrição da tabela, e o adaptador JDBC de eventos ainda não existe.
3. **Escopo do ProvisionadorDeBanco.** Ele cria o esquema e os papéis, mas não substitui um mecanismo de migração versionada.

### 7.5 Matriz atual: regra → caso → teste → status

| Regra | Caso | Teste (classe#método) | Situação no plano | Status green | Observação |
|-------|------|-----------------------|-------------------|--------------|------------|
| RN-01 | CT-01.01 | `IdentidadeTest#rn01_ct01_01_cadastroPermiteAutenticar` | Bloqueado: quais campos são obrigatórios e o que é "válido" não está definido (LG-01) | **VERDE** |  |
| RN-01 | CT-01.02 | `IdentidadeTest#rn01_ct01_02_campoObrigatorioAusente` | Bloqueado: campos e código de erro indefinidos (LG-01, AM-03) | **DESABILITADO** | Bloqueado: LG-01, campos obrigatórios e código de erro do cadastro não definidos |
| RN-01 | CT-01.03 | `IdentidadeTest#rn01_ct01_03_emailDuplicado` | Bloqueado: a unicidade do e-mail não está escrita em lugar nenhum (QT-01) | **DESABILITADO** | Bloqueado: QT-01, unicidade do e-mail não escrita em nenhum artefato |
| RN-01 | CT-01.04 | `IdentidadeTest#rn01_ct01_04_contaSemCredencial` | Bloqueado: a regra só existe implicitamente (LG-01) | **DESABILITADO** | Bloqueado: LG-01, regra de conta sem credencial só implícita |
| RN-02 | CT-02.01 | `IdentidadeTest#rn02_ct02_01_credencialArmazenadaComoHash` | Pronto (mecanismo) | **VERDE** |  |
| RN-02 | CT-02.02 | `IdentidadeTest#rn02_ct02_02_senhaCorretaAutentica` | Pronto | **VERDE** |  |
| RN-02 | CT-02.03 | `IdentidadeTest#rn02_ct02_03_senhaErradaNaoAutentica` | Bloqueado: código e corpo da resposta de falha de login indefinidos (AM-03) | **VERDE** |  |
| RN-02 | CT-02.04 | `IdentidadeTest#rn02_ct02_04_senhaNuncaEmLog` | Condicional (ADR-015: "nenhum token, senha ou e-mail em log") | **VERDE** |  |
| RN-02 | CT-02.05 | `IdentidadeTest#rn02_ct02_05_senhaForaDaPolitica` | Bloqueado: sem política de credencial (L-06) | **DESABILITADO** | Bloqueado: L-06, sem política de credencial |
| RN-03 | (sem caso) | `IdentidadeTest#rn03_semCasoPossivel` | — | **DESABILITADO** | Sem regra: LG-02, o artefato só diz que o perfil existe (docs/arquitetura.md D6.1) |
| RN-04 | CT-04.01 | `IdentidadeTest#rn04_ct04_01_recuperaERevogaSessoes` | Condicional | **VERDE** |  |
| RN-04 | CT-04.02 | `IdentidadeTest#rn04_ct04_02_respostaIdenticaParaEmailInexistente` | Condicional | **VERDE** |  |
| RN-04 | CT-04.03 | `IdentidadeTest#rn04_ct04_03_codigoDeUsoUnico` | Condicional | **VERDE** |  |
| RN-04 | CT-04.04 | `IdentidadeTest#rn04_ct04_04a_codigoAceitoAntesDaValidade`<br>`IdentidadeTest#rn04_ct04_04b_codigoRejeitadoAposValidade`<br>`IdentidadeTest#rn04_ct04_04c_codigoNaBordaDaValidade` | Bloqueado: `T_rec` (L-05) | **VERDE** | 3 execuções |
| RN-04 | CT-04.05 | `IdentidadeTest#rn04_ct04_05_codigoAdulteradoNaoRevoga` | Condicional | **VERDE** |  |
| RN-04 | CT-04.06 | `IdentidadeTest#rn04_ct04_06_codigoGuardadoSoComoHash` | Condicional | **VERDE** |  |
| RN-04 | CT-04.07 | `IdentidadeTest#rn04_ct04_07_doisCodigosEmSequencia` | Bloqueado: não se sabe se o novo invalida o anterior (QT-02) | **DESABILITADO** | Bloqueado: QT-02, novo código invalida o anterior? |
| RN-04 | CT-04.08 | `IdentidadeTest#rn04_ct04_08_recuperacaoRevogaSoDoTitular` | Novo (Aula 07): lacuna de risco do JaCoCo | **VERDE** | cobre o filtro por usuário de `ServicoDeSessao.revogarTodas` |
| RN-04 | CT-04.09 | `IdentidadeTest#rn04_ct04_09_redefinicaoSemCodigo` | Novo (Aula 07): lacuna de risco do JaCoCo | **VERDE** |  |
| RN-05 | CT-05.01 | `SessaoTest#rn05_ct05_01_loginCriaSessaoEEmitePar` | Condicional | **VERDE** |  |
| RN-05 | CT-05.02 | `SessaoTest#rn05_ct05_02_doisDispositivosDuasSessoes` | Condicional | **VERDE** |  |
| RN-05 | CT-05.03 | `SessaoTest#rn05_ct05_03_credencialInvalidaNaoCriaSessao` | Condicional (o código de resposta é AM-03) | **VERDE** |  |
| RN-05 | CT-05.04 | `SessaoTest#rn05_ct05_04_novoLoginMesmoDispositivo` | Bloqueado: não se sabe se substitui ou cria outra sessão (QT-03, AM-10) | **DESABILITADO** | Bloqueado: QT-03 / AM-10, novo login no mesmo dispositivo substitui ou cria sessão? |
| RN-05 | CT-05.05 | `SessaoTest#rn05_ct05_05_limiteDeSessoes` | Bloqueado: limite de sessões indefinido (Q-05) | **DESABILITADO** | Bloqueado: Q-05, limite de sessões não definido |
| RN-05 | CT-05.06 | `IdentidadeTest#rn05_ct05_06_credencialAusente` (parametrizado: e-mail nulo, senha nula) | Novo (Aula 07): lacuna de risco do JaCoCo | **VERDE** |  |
| RN-06 | CT-06.01 | `SessaoTest#rn06_ct06_01_tokenValidoIdentificaUsuario` | Condicional | **VERDE** |  |
| RN-06 | CT-06.02 | `SessaoTest#rn06_ct06_02_aceitoAntesDoTtl` | Bloqueado: `TTL_acesso` (Q-04) | **VERDE** |  |
| RN-06 | CT-06.03 | `SessaoTest#rn06_ct06_03_bordaDoTtl` | Desbloqueado (Aula 07, §9) | **VERDE** | no instante exato do TTL → `token_expirado`; com CT-06.02 e CT-06.04 completa a trinca −1 ms / 0 / +1 ms |
| RN-06 | CT-06.04 | `SessaoTest#rn06_ct06_04_expiradoAposTtl` | Bloqueado: `TTL_acesso` | **VERDE** |  |
| RN-06 | CT-06.05 | `SessaoTest#rn06_ct06_05_tokenMalformado` | Condicional | **VERDE** |  |
| RN-06 | CT-06.06 | `SessaoTest#rn06_ct06_06_tokenInexistente` | Condicional | **VERDE** |  |
| RN-06 | CT-06.07 | `SessaoTest#rn06_ct06_07_tokenDeSessaoRevogada` | Condicional | **VERDE** | **(Aula 07)** resultado esperado mudou de `token_invalido` para `token_revogado` (§9) |
| RN-06 | CT-06.08 | `SessaoTest#rn06_ct06_08_expiradoERevogado` | Desbloqueado (Aula 07, §9) | **VERDE** | `token_expirado` prevalece (ordem da tabela E2; interpretação) |
| RN-06 | CT-06.09 | `SessaoTest#rn06_ct06_09_tokenIlegivel` (parametrizado: HMAC vazio, id ilegível, versão não numérica) | Novo (Aula 07): lacuna de risco do JaCoCo | **VERDE** | os casos nulo e vazio passaram para o CT-06.10 (§9) |
| RN-06 | CT-06.10 | `SessaoTest#rn06_ct06_10_tokenAusente` (parametrizado: nulo, vazio, só espaços) | Novo (Aula 07, §9) | **VERDE** | `token_ausente` |
| RN-06 | CT-06.11 | `SessaoTest#rn06_ct06_11_expiradoNegaMesmoComAssinaturaAtiva` | Novo (Aula 07, §9) | **VERDE** | a política liberaria o exclusivo; o token expirado nega antes |
| RN-07 | CT-07.01 | `SessaoTest#rn07_ct07_01_renovacaoRotaciona` | Condicional | **VERDE** |  |
| RN-07 | CT-07.02 | `SessaoTest#rn07_ct07_02_reusoDentroDaTolerancia` | Bloqueado: `T_tol` (EXP-10) | **VERDE** |  |
| RN-07 | CT-07.03 | `SessaoTest#rn07_ct07_03_reusoForaDaToleranciaRevoga` | Bloqueado: `T_tol` | **VERDE** |  |
| RN-07 | CT-07.04 | `SessaoTest#rn07_ct07_04_reusoDeOutroDispositivoRevoga` | Condicional; AM-10 (como se identifica "dispositivo") | **VERDE** |  |
| RN-07 | CT-07.05 | `SessaoTest#rn07_ct07_05_refreshMaisAntigoRevoga` | Condicional | **VERDE** |  |
| RN-07 | CT-07.06 | `SessaoTest#rn07_ct07_06a_renovacaoAntesDoTtlRefresh`<br>`SessaoTest#rn07_ct07_06b_renovacaoAposTtlRefresh` | Bloqueado: `TTL_refresh` (Q-04) | **VERDE** | 2 execuções |
| RN-07 | CT-07.07 | `SessaoTest#rn07_ct07_07_refreshInvalido` | Condicional | **VERDE** |  |
| RN-07 | CT-07.08 | `SessaoTest#rn07_ct07_08_nuncaDoisParesValidos` | Condicional | **VERDE** |  |
| RN-07 | CT-07.09 | `SessaoTest#rn07_ct07_09_refreshForjadoNaoRevoga` | Novo (Aula 07): lacuna de risco do JaCoCo | **VERDE** |  |
| RN-08 | CT-08.01 | `SessaoTest#rn08_ct08_01_corridaNaMesmaSessao` | Condicional (depende de `T_tol`) | **VERDE** |  |
| RN-08 | CT-08.02 | `SessaoTest#rn08_ct08_02_corridaEmDispositivosDiferentes` | Condicional | **VERDE** |  |
| RN-08 | CT-08.03 | `SessaoTest#rn08_ct08_03_umUnicoParAposCorrida` | Condicional | **VERDE** |  |
| RN-09 | CT-09.01 | `SessaoTest#rn09_ct09_01_logoutInvalidaTokenNaHora` | Condicional | **VERDE** |  |
| RN-09 | CT-09.02 | `SessaoTest#rn09_ct09_02_logoutInvalidaRefresh` | Condicional | **VERDE** |  |
| RN-09 | CT-09.03 | `SessaoTest#rn09_ct09_03_logoutSoDoDispositivoAtual` | Condicional; Q-05 (logout global) pode mudar | **VERDE** |  |
| RN-09 | CT-09.04 | `SessaoTest#rn09_ct09_04_logoutComTokenInvalido` | Bloqueado: resposta indefinida (QT-05) | **DESABILITADO** | Bloqueado: QT-05, resposta do logout com token já inválido não definida |
| RN-09 | CT-09.05 | `SessaoTest#rn09_ct09_05_sessaoRevogadaNaoRessuscita` | Condicional | **VERDE** |  |
| RN-09 | CT-09.06 | `SessaoTest#rn09_ct09_06_logoutConcorrenteComRenovacao` | Novo (Aula 07): lacuna de risco do JaCoCo | **VERDE** | corrida determinística com dublê, sem threads; relógio `Clock.fixed` |
| RN-09 | CT-09.07 | `SessaoTest#rn09_ct09_07_logoutConcorrenteComRevogacao` | Novo (Aula 07): lacuna de risco do JaCoCo | **VERDE** | relógio `Clock.fixed` |
| RN-10 | CT-10.01 | `AssinaturaTest#rn10_ct10_01_planoInicial` | Bloqueado: não se sabe se todo cadastro começa no free (QT-06) | **DESABILITADO** | Bloqueado: QT-06, todo cadastro começa no free? |
| RN-10 | CT-10.02 | `AssinaturaTest#rn10_ct10_02_soFreeEPremium` | Pronto (só existem esses dois: `README.md:11`) | **VERDE** |  |
| RN-10 | CT-10.03 | `AssinaturaTest#rn10_ct10_03_doisPlanosVigentes` | Bloqueado: não está escrito (QT-07) | **DESABILITADO** | Bloqueado: QT-07, mais de uma assinatura vigente é permitida? |
| RN-11 | CT-11.01 | `AssinaturaTest#rn11_ct11_01_dentroDaVigencia` | Pronto | **VERDE** |  |
| RN-11 | CT-11.02 | `AssinaturaTest#rn11_ct11_02_antesDoInicio` | Pronto | **VERDE** |  |
| RN-11 | CT-11.03 | `AssinaturaTest#rn11_ct11_03_bordaDoInicio` | Bloqueado | **DESABILITADO** | Bloqueado: AM-01, no instante exato do início a vigência vale? |
| RN-11 | CT-11.04 | `AssinaturaTest#rn11_ct11_04_antesDoFim` | Pronto | **VERDE** |  |
| RN-11 | CT-11.05 | `AssinaturaTest#rn11_ct11_05_bordaDoFim` | Bloqueado | **DESABILITADO** | Bloqueado: AM-01, no instante exato do fim a vigência vale? |
| RN-11 | CT-11.06 | `AssinaturaTest#rn11_ct11_06_aposOFim` | Pronto | **VERDE** |  |
| RN-11 | CT-11.07 | `AssinaturaTest#rn11_ct11_07_fimAntesDoInicio` | Bloqueado: a validação não está escrita (QT-08) | **DESABILITADO** | Bloqueado: QT-08, validação de fim < início não escrita |
| RN-12 | CT-12.01 | `AssinaturaTest#rn12_ct12_01_upgradeLiberaExclusivo` | Bloqueado: o mecanismo do upgrade é desconhecido (Q-02; S-04) | **DESABILITADO** | Bloqueado: Q-02 / S-04, mecanismo do upgrade sem pagamento real desconhecido |
| RN-12 | CT-12.02 | `AssinaturaTest#rn12_ct12_02_upgradeDePremium` | Bloqueado (QT-09) | **DESABILITADO** | Bloqueado: QT-09, premium vigente pode pedir upgrade? |
| RN-12 | CT-12.03 | `AssinaturaTest#rn12_ct12_03_upgradeDuplicado` | Bloqueado (QT-07) | **DESABILITADO** | Bloqueado: QT-07, upgrade pode gerar duas assinaturas vigentes? |
| RN-13 | CT-13.01 | `AssinaturaTest#rn13_ct13_01_exclusivoNegadoAposCorte` | Bloqueado: `t_corte` (Q-03: imediato ou fim da vigência?) | **DESABILITADO** | Bloqueado: Q-03, cancelamento corta na hora ou no fim da vigência (t_corte)? |
| RN-13 | CT-13.02 | `AssinaturaTest#rn13_ct13_02_bordaDoCorte` | Bloqueado: Q-03; AM-01 para `= t_corte` | **DESABILITADO** | Bloqueado: Q-03 (t_corte) e AM-01 (borda) |
| RN-13 | CT-13.03 | `AssinaturaTest#rn13_ct13_03_publicoAposCancelar` | Pronto | **VERDE** |  |
| RN-13 | CT-13.04 | `AssinaturaTest#rn13_ct13_04_contaESessaoAposCancelar` | Pronto | **VERDE** |  |
| RN-13 | CT-13.05 | `AssinaturaTest#rn13_ct13_05_cancelarFree` | Bloqueado (QT-10) | **DESABILITADO** | Bloqueado: QT-10, cancelar o plano free |
| RN-13 | CT-13.06 | `AssinaturaTest#rn13_ct13_06_cancelarDuasVezes` | Bloqueado (QT-10) | **DESABILITADO** | Bloqueado: QT-10, cancelar duas vezes |
| RN-14 | CT-14.01 | `AssinaturaTest#rn14_ct14_01_tokenNaoCarregaPrivilegio` | Pronto | **VERDE** |  |
| RN-14 | CT-14.02 | `AssinaturaTest#rn14_ct14_02_upgradeSemNovoLogin` | Bloqueado pelo mecanismo de upgrade (Q-02) | **DESABILITADO** | Bloqueado: Q-02, mecanismo do upgrade |
| RN-15 | CT-15.01 | `AcessoAoConteudoTest#rn15_ct15_01_anonimoLePublico` | Pronto | **VERDE** |  |
| RN-15 | CT-15.02 | `AcessoAoConteudoTest#rn15_ct15_02_todosOsPerfisLeemPublico` | Pronto | **VERDE** | 3 execuções |
| RN-15 | CT-15.03 | `AcessoAoConteudoTest#rn15_ct15_03_tokenInvalidoEmPublico` | Bloqueado: 401 ou tratar como anônimo? (QT-11) | **DESABILITADO** | Bloqueado: QT-11, token inválido em rota pública dá 401 ou é tratado como anônimo? |
| RN-16 | CT-16.01 | `AcessoAoConteudoTest#rn16_ct16_01_premiumVigenteLeExclusivo` | Pronto | **VERDE** |  |
| RN-16 | CT-16.02 | `AcessoAoConteudoTest#rn16_ct16_02_anonimoEmExclusivo` | Pronto (o nome do código é condicional, ADR-014) | **VERDE** |  |
| RN-16 | CT-16.03 | `AcessoAoConteudoTest#rn16_ct16_03_freeEmExclusivo` | Pronto (o nome do código é condicional) | **VERDE** |  |
| RN-16 | CT-16.04 | `AcessoAoConteudoTest#rn16_ct16_04_premiumVencidoEmExclusivo` | Pronto | **VERDE** |  |
| RN-16 | CT-16.05 | `AcessoAoConteudoTest#rn16_ct16_05_negacaoSemConteudo` | Pronto (CEN-01: "sem nenhum campo do conteúdo exclusivo no corpo") | **VERDE** |  |
| RN-16 | CT-16.06 | `AcessoAoConteudoTest#rn16_ct16_06_listaMista` | Bloqueado: não se sabe se eles são omitidos ou listados sem corpo (QT-12) | **DESABILITADO** | Bloqueado: QT-12, em listas os exclusivos são omitidos ou listados sem corpo? |
| RN-16 | CT-16.07 | `AcessoAoConteudoTest#rn16_ct16_07_freeAlemDoAnonimo` | Bloqueado (Q-19) | **DESABILITADO** | Bloqueado: Q-19, o que o free vê a mais que o anônimo |
| RN-17 | CT-17.01 | `DadosDeUsuarioTest#rn17_ct17_01_propriosDadosPermitidos` | Pronto | **VERDE** |  |
| RN-17 | CT-17.02 | `DadosDeUsuarioTest#rn17_ct17_02_anonimoEmDadosDeConta` | Pronto (o código específico não está definido na matriz: AM-03) | **VERDE** |  |
| RN-17 | CT-17.03 | `DadosDeUsuarioTest#rn17_ct17_03_premiumVencidoPropriosDados` | Pronto | **VERDE** |  |
| RN-17 | CT-17.04 | `DadosDeUsuarioTest#rn17_ct17_04_principalNulo` | Novo (Aula 07): lacuna de risco do JaCoCo | **VERDE** |  |
| RN-18 | CT-18.01 | `DadosDeUsuarioTest#rn18_ct18_01_freeEmOutroUsuario` | Pronto | **VERDE** |  |
| RN-18 | CT-18.02 | `DadosDeUsuarioTest#rn18_ct18_02_premiumEmOutroUsuario` | Pronto | **VERDE** |  |
| RN-18 | CT-18.03 | `DadosDeUsuarioTest#rn18_ct18_03_anonimoEmOutroUsuario` | Pronto | **VERDE** |  |
| RN-18 | CT-18.04 | `DadosDeUsuarioTest#rn18_ct18_04_usuarioInexistente` | Bloqueado: 403 ou 404? O 404 revelaria a existência de contas (QT-13) | **DESABILITADO** | Bloqueado: QT-13, usuário inexistente: 403 ou 404? |
| RN-19 | CT-19.01 | `AcessoAoConteudoTest#rn19_ct19_01_semClassificacaoNegado` | Pronto | **VERDE** |  |
| RN-19 | CT-19.02 | `AcessoAoConteudoTest#rn19_ct19_02_todoCaminhoPassaPelaPolitica` | Pronto | **VERDE** |  |
| RN-19 | CT-19.03 | `AcessoAoConteudoTest#rn19_ct19_03_matrizCompleta` | Pronto, exceto as células do free (Q-19) | **VERDE** | 8 execuções |
| RN-19 | CT-19.04 | `DadosDeUsuarioTest#rn19_ct19_04_dadoSemDonoNegado` | Novo (Aula 07): lacuna de risco do JaCoCo | **VERDE** |  |
| RN-19 | CT-19.05 | `AcessoAoConteudoTest#rn19_ct19_05_listagemRespeitaAPolitica` | Novo (Aula 07): lacuna de risco do JaCoCo | **VERDE** | asserção válida nas duas leituras de QT-12 |
| RN-20 | (sem caso) | `AcessoAoConteudoTest#rn20_semCasoPossivel` | — | **DESABILITADO** | Sem regra: LG-03, nenhum artefato define regra sobre clubes, jogos, resultados ou matérias |
| RN-21 | CT-21.01 | `ContratoDeErroTest#rn21_ct21_01_problemDetailsComCodigoEstavel` | Condicional (Q-07) | **VERDE** | 5 execuções |
| RN-21 | CT-21.02 | `ContratoDeErroTest#rn21_ct21_02_mesmaSituacaoMesmoCodigo` | Condicional | **VERDE** |  |
| RN-21 | CT-21.03 | `ContratoDeErroTest#rn21_ct21_03_semDetalheInterno` | Condicional | **VERDE** |  |
| RS-01 | CT-S01.01 | `JanelaIncrementalTest#rs01_cts01_01_eventosDentroDaJanela` | Condicional | **VERDE** |  |
| RS-01 | CT-S01.02 | `JanelaIncrementalTest#rs01_cts01_02_noWatermarkFora` | Condicional | **VERDE** |  |
| RS-01 | CT-S01.03 | `JanelaIncrementalTest#rs01_cts01_03_logoAposWatermarkDentro` | Condicional | **VERDE** |  |
| RS-01 | CT-S01.04 | `JanelaIncrementalTest#rs01_cts01_04_noCorteDentro` | Condicional | **VERDE** |  |
| RS-01 | CT-S01.05 | `JanelaIncrementalTest#rs01_cts01_05_aposCorteFora` | Condicional | **VERDE** |  |
| RS-01 | CT-S01.06 | `JanelaIncrementalTest#rs01_cts01_06_eventoTardioEntra` | Condicional | **VERDE** |  |
| RS-01 | CT-S01.07 | `OrquestradorDoJobTest#rs01_cts01_07_eventoConsumidoNaoReaparece` | Condicional | **VERDE** |  |
| RS-01 | CT-S01.08 | `OrquestradorDoJobTest#rs01_cts01_08_corteEInicioMenosMargem` | Bloqueado: `Δ` (Q-15) | **VERDE** |  |
| RS-02 | CT-S02.01 | `OrquestradorDoJobTest#rs02_cts02_01_concluidaAvancaCheckpoint` | Pronto | **VERDE** |  |
| RS-02 | CT-S02.02 | `OrquestradorDoJobTest#rs02_cts02_02_falhaMantemCheckpoint` | Pronto | **VERDE** |  |
| RS-02 | CT-S02.03 | `OrquestradorDoJobTest#rs02_cts02_03_reprovadaMantemCheckpoint` | Condicional (ADR-011) | **VERDE** |  |
| RS-02 | CT-S02.04 | `OrquestradorDoJobTest#rs02_cts02_04_descarteAvancaCheckpoint` | Condicional (ADR-011) | **VERDE** |  |
| RS-02 | CT-S02.05 | `OrquestradorDoJobTest#rs02_cts02_05_checkpointSoDeExecucaoTerminal` | Pronto | **VERDE** |  |
| RS-02 | CT-S02.06 | `PersistenciaDoTreinoIntegracaoTest#rs02_cts02_06_reprovadaNaoMoveCheckpointNoBanco` | Novo (Aula 07): lacuna de risco do JaCoCo | **VERDE** |  |
| RS-03 | CT-S03.01 | `OrquestradorDoJobTest#rs03_cts03_01_concluidaSemEfeito` | Pronto | **VERDE** |  |
| RS-03 | CT-S03.02 | `OrquestradorDoJobTest#rs03_cts03_02_retomadaComMesmaJanela` | Pronto | **VERDE** |  |
| RS-03 | CT-S03.03 | `PersistenciaDoTreinoIntegracaoTest#rs03_cts03_03_dataReferenciaUnica` | Pronto | **VERDE** |  |
| RS-04 | CT-S04.01 | `JanelaIncrementalTest#rs04_cts04_01_registradorNaoDuplica` | Condicional | **VERDE** |  |
| RS-04 | CT-S04.02 | `JanelaIncrementalTest#rs04_cts04_02_extratorDeduplica` | Condicional | **VERDE** |  |
| RS-04 | CT-S04.03 | `JanelaIncrementalTest#rs04_cts04_03_duplicataDeNegocio` | Bloqueado: chave de negócio (Q-09; RA-02) | **DESABILITADO** | Bloqueado: Q-09 / RA-02, chave de negócio de deduplicação depende do Modelo M |
| RS-05 | CT-S05.01 | `OrquestradorDoJobTest#rs05_cts05_01_lockIndisponivelSemEfeito` | Pronto | **VERDE** |  |
| RS-05 | CT-S05.02 | `PersistenciaDoTreinoIntegracaoTest#rs05_cts05_02_quedaLiberaLock` | Pronto | **VERDE** |  |
| RS-05 | CT-S05.03 | `PersistenciaDoTreinoIntegracaoTest#rs05_cts05_03_umLockPorVez` | Pronto | **VERDE** |  |
| RS-05 | CT-S05.04 | `PersistenciaDoTreinoIntegracaoTest#rs05_cts05_04_liberarPermiteAProxima` | Novo (Aula 07): lacuna de risco do JaCoCo | **VERDE** |  |
| RS-06 | CT-S06.01 | `OrquestradorDoJobTest#rs06_cts06_01_falhaAposCandidato` | Pronto | **VERDE** |  |
| RS-06 | CT-S06.02 | `OrquestradorDoJobTest#rs06_cts06_02_orfaoArquivadoNaSeguinte` | Pronto | **VERDE** |  |
| RS-06 | CT-S06.03 | `OrquestradorDoJobTest#rs06_cts06_03a_transitorioRecupera`<br>`OrquestradorDoJobTest#rs06_cts06_03b_transitorioEsgota` | Bloqueado: número de tentativas indefinido | **VERDE** | 2 execuções |
| RS-06 | CT-S06.04 | `OrquestradorDoJobTest#rs06_cts06_04_erroDeDadosSemRetry` | Pronto | **VERDE** |  |
| RS-07 | CT-S07.01 | `ValidadorDeDadosTest#rs07_cts07_01_esquemasNeNMenos1` | Condicional | **VERDE** |  |
| RS-07 | CT-S07.02 | `ValidadorDeDadosTest#rs07_cts07_02_esquemaDesconhecidoEmQuarentena` | Condicional | **VERDE** |  |
| RS-07 | CT-S07.03 | `ValidadorDeDadosTest#rs07_cts07_03c_igualAoLimite`<br>`ValidadorDeDadosTest#rs07_cts07_03b_acimaDoLimite`<br>`ValidadorDeDadosTest#rs07_cts07_03a_abaixoDoLimite` | Bloqueado: `L_quar` | **VERDE** | 3 execuções |
| RS-07 | CT-S07.04 | `OrquestradorDoJobTest#rs07_cts07_04_treinoSemEsquemaDesconhecido` | Condicional | **VERDE** |  |
| RS-07 | CT-S07.05 | `ValidadorDeDadosTest#rs07_cts07_05_volumeMinimo` | Bloqueado: volume mínimo indefinido | **DESABILITADO** | Bloqueado: volume mínimo da janela não definido (§6.3 item 8) |
| RS-07 | CT-S07.06 | `OrquestradorDoJobTest#rs07_cts07_06_quarentenaNoJob` (trinca −1/0/+1) | Novo (Aula 07): lacuna de risco do JaCoCo | **VERDE** | no limite exato segue AM-02 (provisório, como CT-S07.03c) |
| RS-08 | CT-S08.01 | `GateDePromocaoTest#rs08_cts08_01_candidatoMelhorAprova` | Bloqueado (métrica, Q-09) | **VERDE** |  |
| RS-08 | CT-S08.02 | `GateDePromocaoTest#rs08_cts08_02_exatamenteNaToleranciaAprova` | Bloqueado (`tol_gate`) | **VERDE** |  |
| RS-08 | CT-S08.03 | `GateDePromocaoTest#rs08_cts08_03_abaixoDaToleranciaReprova` | Bloqueado | **VERDE** |  |
| RS-08 | CT-S08.04 | `GateDePromocaoTest#rs08_cts08_04_sanidadeEDadosSaoObrigatorios` | Pronto (a sanidade é condição obrigatória) | **VERDE** |  |
| RS-08 | CT-S08.05 | `GateDePromocaoTest#rs08_cts08_05b_primeiraExecucaoAbaixoDoPiso`<br>`GateDePromocaoTest#rs08_cts08_05a_primeiraExecucaoNoPiso` | Bloqueado (`piso`) | **VERDE** | 2 execuções |
| RS-09 | CT-S09.01 | `OrquestradorDoJobTest#rs09_cts09_01_reprovacao` | Condicional | **VERDE** |  |
| RS-09 | CT-S09.02 | `OrquestradorDoJobTest#rs09_cts09_02_proximaUsaAtivoEJanelaAcumulada` | Condicional | **VERDE** |  |
| RS-09 | CT-S09.03 | `OrquestradorDoJobTest#rs09_cts09_03_limiteK` | Bloqueado: `K` | **VERDE** |  |
| RS-09 | CT-S09.04 | `OrquestradorDoJobTest#rs09_cts09_04a_descarteAntesDeK`<br>`OrquestradorDoJobTest#rs09_cts09_04b_descarteSemResponsavel`<br>`OrquestradorDoJobTest#rs09_cts09_04c_descarteComResponsavelNuloOuVazio` (Aula 07) | Condicional | **VERDE** | 2 execuções |
| RS-09 | CT-S09.05 | `OrquestradorDoJobTest#rs09_cts09_05_contadorZera` | Bloqueado: não está escrito que zera (QT-14) | **DESABILITADO** | Bloqueado: QT-14, o contador de reprovações consecutivas zera numa aprovação? |
| RS-09 | CT-S09.06 | `OrquestradorDoJobTest#rs09_cts09_06_descarteSoDaMaisRecente` | Novo (Aula 07): lacuna de risco do JaCoCo | **VERDE** |  |
| RS-09 | CT-S09.07 | `OrquestradorDoJobTest#rs09_cts09_07a_descarteDeExecucaoInexistente`<br>`OrquestradorDoJobTest#rs09_cts09_07b_descarteDeExecucaoConcluida` | Novo (Aula 07): lacuna de risco do JaCoCo | **VERDE** |  |
| RS-10 | CT-S10.01 | `OrquestradorDoJobTest#rs10_cts10_01_promocao` | Pronto | **VERDE** |  |
| RS-10 | CT-S10.02 | `PersistenciaDoTreinoIntegracaoTest#rs10_cts10_02_transacaoFinalTudoOuNada` | Pronto | **VERDE** | a asserção do checkpoint passou a valer de fato depois da correção do isolamento (§8.4) |
| RS-10 | CT-S10.03 | `OrquestradorDoJobTest#rs10_cts10_03_ponteiroECheckpointJuntos` | Pronto | **VERDE** |  |
| RS-10 | CT-S10.04 | `PersistenciaDoTreinoIntegracaoTest#rs10_cts10_04_transacaoFinalConfirmada` | Novo (Aula 07): lacuna de risco do JaCoCo | **VERDE** | revelou o defeito D-01 (§8.4), corrigido |
| RS-11 | CT-S11.01 | `VersoesEPonteiroTest#rs11_cts11_01_rollback` | Pronto | **VERDE** |  |
| RS-11 | CT-S11.02 | `VersoesEPonteiroTest#rs11_cts11_02_ponteiroSoParaVersaoAprovada` | Pronto | **VERDE** | 3 execuções |
| RS-11 | CT-S11.03 | `PersistenciaDoTreinoIntegracaoTest#rs11_cts11_03_apiNaoAlteraPonteiro` | Pronto | **VERDE** |  |
| RS-11 | CT-S11.04 | `VersoesEPonteiroTest#rs11_cts11_04_rollbackSemAnterior` | Bloqueado: comportamento indefinido (QT-15) | **DESABILITADO** | Bloqueado: QT-15, rollback sem versão anterior é proibido ou volta para sem modelo? |
| RS-11 | CT-S11.05 | `VersoesEPonteiroTest#rs11_cts11_05_transicoesDeVersao` | Pronto (§6.4 lista só as transições permitidas) | **VERDE** | 25 execuções |
| RS-11 | CT-S11.06 | `VersoesEPonteiroTest#rs11_cts11_06_acaoManualExigeResponsavel` (parametrizado: nulo, vazio, brancos) | Novo (Aula 07): lacuna de risco do JaCoCo | **VERDE** |  |
| RS-11 | CT-S11.07 | `VersoesEPonteiroTest#rs11_cts11_07_versaoInexistente` | Novo (Aula 07): lacuna de risco do JaCoCo | **VERDE** |  |
| RS-11 | CT-S11.08 | `PersistenciaDoTreinoIntegracaoTest#rs11_cts11_08_trocaManualPersistida` | Novo (Aula 07): lacuna de risco do JaCoCo | **VERDE** |  |
| RS-12 | CT-S12.01 | `PersistenciaDoTreinoIntegracaoTest#rs12_cts12_01_versaoImutavel` | Pronto | **VERDE** |  |
| RS-12 | CT-S12.02 | `PersistenciaDoTreinoIntegracaoTest#rs12_cts12_02_artefatoNaoSobrescrito` | Condicional (ADR-013) | **DESABILITADO** | Bloqueado: L-11, tecnologia do Armazenamento de Artefatos adiada (ADR-013) |
| RS-13 | CT-S13.01 | `LeitorDoModeloAtivoTest#rs13_cts13_01_trocaAposCargaValida` | Condicional | **VERDE** |  |
| RS-13 | CT-S13.02 | `LeitorDoModeloAtivoTest#rs13_cts13_02_corrompidoMantemAtual` | Condicional | **VERDE** |  |
| RS-13 | CT-S13.03 | `LeitorDoModeloAtivoTest#rs13_cts13_03_fallbackSemModelo` | Condicional | **VERDE** |  |
| RS-13 | CT-S13.04 | `LeitorDoModeloAtivoTest#rs13_cts13_04_artefatoAusenteMantemAtual` | Condicional | **VERDE** |  |
| RS-14 | CT-S14.01 | `OrquestradorDoJobTest#rs14_cts14_01_baseEOAtivo` | Pronto | **VERDE** |  |
| RS-14 | CT-S14.02 | `OrquestradorDoJobTest#rs14_cts14_02_semDadosAntigos` | Pronto | **VERDE** |  |
| RS-15 | CT-S15.01 | `PersistenciaDoTreinoIntegracaoTest#rs15_cts15_01_batchLeitorSemAcessoAApp` | Condicional | **VERDE** |  |
| RS-15 | CT-S15.02 | `PersistenciaDoTreinoIntegracaoTest#rs15_cts15_02_batchLeitorLeEventos` | Condicional | **VERDE** |  |
| RS-15 | CT-S15.03 | `PersistenciaDoTreinoIntegracaoTest#rs15_cts15_03_apiNaoEscreveEmMl` | Condicional | **VERDE** |  |


> **Aula 07:** a matriz acima já inclui os 19 casos novos (situação "Novo (Aula 07)") e o CT-S09.04c. Total: **136 casos verdes**, 34 desabilitados (os mesmos de antes) e nenhum `@Disabled` novo.

## 8. Cobertura com JaCoCo (Aula 07)

> **Execução:** `./mvnw -B clean verify`, em 2026-11-13: **BUILD SUCCESS**, com 230 execuções, 0 falhas, 0 erros e 36 desabilitadas. O gate de cobertura passou ("All coverage checks have been met").

### 8.1 Configuração

| Item | Valor |
|------|-------|
| Plugin | `org.jacoco:jacoco-maven-plugin:0.8.15` no `pom.xml` |
| `prepare-agent` | Instrumenta a suíte do Surefire. Não há `forkCount=0`, então o agente é carregado. |
| `report` + `check` | Rodam na fase `verify`. Relatórios em `target/site/jacoco/`: `index.html`, `jacoco.xml` (usado pelo SonarCloud) e `jacoco.csv` |
| Gate | Bundle inteiro: **LINE ≥ 0,80** e **BRANCH ≥ 0,70**. Abaixo disso, `verify` falha. |
| CI | Step "Relatório de cobertura (JaCoCo)", com `if: always()`. O artifact `cobertura-jacoco-<run_id>` fica guardado 7 dias e existe também quando o gate reprova, porque o `report` roda antes do `check`. |

### 8.2 Resultado

| Medida | Antes (só JaCoCo ligado) | Depois dos testes de risco |
|--------|--------------------------|----------------------------|
| Linhas | 91,8% | **97,3%** (585/601) |
| Branches | 80,3% | **90,1%** (200/222) |
| Execuções de teste | 199 | 230 (+31, em 21 métodos novos) |
| Casos verdes na matriz | 117 | 136 |

O total já passava da meta antes dos testes novos. Os testes foram escritos porque havia **regra sem verificação**, e não para subir o percentual.

### 8.3 Lacunas encontradas e classificação

Leitura do `jacoco.xml` por classe, só das classes com regra de negócio. **Risco** = um defeito ali viola uma regra do inventário (§2). **Cosmética** = código defensivo, inalcançável pelo fluxo ou falha de infraestrutura sem regra. **Bloqueada** = o comportamento depende de uma pergunta em aberto, então testar fixaria uma decisão provisória.

| Classe:linha | O que não era executado | Classe | Tratamento |
|--------------|-------------------------|--------|------------|
| ServicoDeSessao:164-165, 168, 173-174 | Token nulo, HMAC vazio, id ou versão ilegível | **Risco** (RN-06, E2) | CT-06.09 |
| ServicoDeSessao:100 | Refresh antigo **forjado** (não pode revogar a sessão legítima) | **Risco** (RN-07) | CT-07.09 |
| ServicoDeSessao:116 | `revogarTodas` ignorando sessões de outro usuário | **Risco** (RN-04) | CT-04.08 |
| ServicoDeSessao:142, 149 | Revogação que perde a corrida otimista e repete | **Risco** (RN-09) | CT-09.06, CT-09.07 |
| ServicoDeSessao:132-133 | Tolerância com `renovadaEm` ou dispositivo nulo | Cosmética (só existe anterior depois de uma renovação, que sempre grava `renovadaEm`) | — |
| ServicoDeSessao:142 (`isEmpty`) | Sessão apagada durante a revogação | Cosmética (não existe exclusão de sessão) | — |
| ServicoDeIdentidade:49-50 | `autenticar` com e-mail ou senha nulos | **Risco** (RN-05) | CT-05.06 |
| ServicoDeIdentidade:72-73 | `redefinirSenha` sem código | **Risco** (RN-04) | CT-04.09 |
| PoliticaDeAcesso:41, 47 | Principal nulo; dado sem dono | **Risco** (RN-17, RN-19) | CT-17.04, CT-19.04 |
| ServicoDeConteudo:40-41 | Listagem com item **liberado** (só havia o caso "nega tudo") | **Risco** (RN-19) | CT-19.05 |
| ServicoDeConteudo:23 | Matéria inexistente | Bloqueada (404 × 403 sem regra, QT-13) | — |
| ServicoDeAssinatura:46 | Cancelar free, cancelar duas vezes, premium vencido | Bloqueada (CT-13.05, CT-13.06, QT-10) | — |
| ServicoDeAssinatura:53 | `upgrade` | Bloqueada (Q-02, RN-12) | — |
| ServicoDeAssinatura:32, 57 | Plano diferente de FREE/PREMIUM; início de vigência nulo | Cosmética (inalcançável / defensiva) | — |
| OrquestradorDoJob:107-108 | Quarentena acima do limite **no Job** (só o validador era testado) | **Risco** (RS-07) | CT-S07.06 (trinca) |
| OrquestradorDoJob:70, 73-78 | Descarte: responsável nulo ou vazio, data inexistente, execução não REPROVADA ou não mais recente | **Risco** (RS-09, ADR-011) | CT-S09.04c, CT-S09.06, CT-S09.07 |
| OrquestradorDoJob:175-176 | Contador de reprovações zera numa aprovação | Bloqueada (QT-14, CT-S09.05) | — |
| OrquestradorDoJob:147, 163 | `maxTentativas < 1`; candidato de execução não FALHOU | Cosmética (configuração inválida; estado só alcançável com queda no meio da execução) | — |
| ValidadorDeDados:27 | Janela vazia | Bloqueada (volume mínimo, CT-S07.05) | — |
| PromotorDeModelo:26-27, 30 | Ação manual sem responsável; versão inexistente | **Risco** (RS-11) | CT-S11.06, CT-S11.07 |
| PromotorDeModelo:21 | Rollback sem versão anterior | Bloqueada (QT-15, CT-S11.04) | — |
| TransicoesDeVersao:21; LeitorDoModeloAtivo:30-31 | Status nulo; recarga da mesma versão | Cosmética (defensiva; otimização sem efeito observável) | — |
| RegistryDeModelosJdbc:85-89, 106, 119, 128, 139, 151-158 | **Nenhum caminho de sucesso** de `finalizar`, `atualizarStatusDaVersao`, `trocarPonteiro` e `versaoAnterior` rodava contra o PostgreSQL | **Risco** (RS-02, RS-10, RS-11) | CT-S02.06, CT-S10.04, CT-S11.08 |
| GerenciadorDeLockPostgres:28-29, 52-63 | `liberar()` nunca executado; reentrância | **Risco** (RS-05) | CT-S05.04 |
| GerenciadorDeLockPostgres:45-46, 58; RegistryDeModelosJdbc:213-214; Hashes:24-25, 53-54; ProvisionadorDeBanco:76-77 | `SQLException` ou algoritmo criptográfico indisponível | Cosmética (falha de infraestrutura, sem regra) | — |
| FootFanaticsApplication:10-11 | `main` | Cosmética | — |

**Técnicas usadas nos testes novos:**
- Limites em trinca −1/0/+1 com `@ParameterizedTest` (CT-S07.06). As demais entradas equivalentes usam `@NullAndEmptySource`, `@ValueSource` e `@CsvSource`.
- Tempo injetado como `java.time.Clock` (`Clock.fixed(T0, UTC)`, adaptado ao `Relogio` por `clock::instant`), sem `Thread.sleep`.
- As corridas da revogação são **determinísticas**: um dublê de repositório executa a operação concorrente entre a leitura e a gravação otimista, sem threads.

**Bordas que continuam fora:** a trinca de tempo do token (`TTL_acesso` −1 ms / 0 / +1 ms) já tem −1 e +1 verdes, mas o ponto 0 depende de AM-01 (CT-06.03, desabilitado). O mesmo vale para vigência, código de recuperação e `t_corte`.

### 8.4 Defeitos encontrados pelos testes novos

| ID | Onde | Defeito | Decisão |
|----|------|---------|---------|
| **D-01** | `RegistryDeModelosJdbc.ponteiro` (produção) | Com `versao_anterior_id` NULL (logo depois da 1ª promoção), `findFirst()` sobre um elemento `null` lançava `NullPointerException`. `versaoAnterior()` quebrava em vez de devolver vazio. | O prompt proibia mudar o comportamento de produção, então a decisão foi perguntada ao usuário. Escolha: **corrigir e registrar**. A correção é de uma linha (a coluna é lida como `Optional` antes do `findFirst`) e não muda nenhuma regra. Coberta por CT-S10.04. |
| **D-02** | `PersistenciaDoTreinoIntegracaoTest.isolarDoTesteAnterior` (teste) | O `TRUNCATE ... CASCADE` do schema `ml` (§7.3) também esvaziava `checkpoint_treino`, que referencia `execucao_treino`. `watermark()` voltava `null`, e a asserção de checkpoint do CT-S10.02 comparava `null` com `null`, sem verificar nada. | Só a preparação mudou: a linha do job é recriada em `epoch`, como o provisionamento a deixa. Asserções, resultados esperados e `@Disabled` não mudaram. O CT-S10.02 passou a verificar o checkpoint de verdade e continua verde. |

### 8.5 Divergência com o material da Aula 07 (resolvida em §9)

O cenário E2 da aula usa os motivos `TOKEN_AUSENTE` e `TOKEN_REVOGADO` e nega o token com **expiração ≤ agora**. A RN-06 (`docs/arquitetura.md:618`) só tem `token_expirado` e `token_invalido`, e o código aceita o token no instante exato da expiração (AM-01). Como o prompt proíbe mudar produção, os testes novos verificam a RN-06 como está. Alinhar a regra à aula exige decidir AM-01 e mudar a RN-06, e fica para a decisão do usuário.

### 8.6 O gate barra a queda de cobertura

| Execução | Comando | Resultado |
|----------|---------|-----------|
| Cobertura derrubada (só 1 classe de teste roda) | `./mvnw -B clean verify -Dtest=ContratoDeErroTest -Dsurefire.failIfNoSpecifiedTests=false` | **BUILD FAILURE**: "lines covered ratio is 0.03, but expected minimum is 0.80" e "branches covered ratio is 0.00, but expected minimum is 0.70" |
| Restaurada (suíte inteira) | `./mvnw -B clean verify` | **BUILD SUCCESS**: "All coverage checks have been met", 97,3% / 90,1% |

**Cuidado observado:** sem `clean`, o `prepare-agent` **acrescenta** dados ao `target/jacoco.exec` da execução anterior. A cobertura antiga mascarou a queda, e o build passou. O CI não sofre com isso porque sempre parte de um checkout limpo. Localmente, use `clean` para medir.


## 9. Regra E2 alinhada ao cenário de qualidade da Aula 07

> **Decisão do usuário (2026-11-14):** alinhar a RN-06 ao cenário E2 do material da Aula 07. A divergência estava registrada em §8.5. **Execução:** `./mvnw -B clean verify`: **BUILD SUCCESS**, 235 execuções, 0 falhas, 34 desabilitadas (2 a menos: CT-06.03 e CT-06.08 foram desbloqueados). Cobertura: 97,1% de linhas e 89,6% de branches. O gate passou.

### 9.1 A regra nova (fonte: cenário E2 da Aula 07)

"Token expirado nunca libera acesso, mesmo quando a assinatura está ativa. Token ausente, inválido ou revogado também é negado. Somente token válido e não expirado permite continuar para as demais decisões de autorização."

| Entrada | Decisão | Código (401) | Antes |
|---------|---------|--------------|-------|
| Ausente (nulo, vazio, só espaços) | Negar | `token_ausente` | `token_invalido` |
| Assinatura inválida (malformado, sessão desconhecida, versão antiga) | Negar | `token_invalido` | igual |
| Expiração ≤ agora | Negar | `token_expirado` | o instante exato **era aceito** (`isAfter`) |
| Revogado | Negar | `token_revogado` | `token_invalido` |
| Válido e vigente | Prosseguir | — | igual |

**Interpretação registrada (QT-04):** a ordem das linhas da tabela foi lida como a ordem de avaliação. Por isso, um token expirado **e** revogado recebe `token_expirado` (CT-06.08). O material não diz isso explicitamente; se a intenção for outra, basta trocar a ordem das duas verificações em `ServicoDeSessao.validar` e o resultado esperado do CT-06.08.

**Fora do escopo (inalterado):** a renovação por refresh continua devolvendo `token_invalido` para sessão revogada (RN-07). O instante exato da expiração do **refresh** e da tolerância de reuso ainda vale (AM-01 continua aberta para eles, para a vigência, o código de recuperação e `t_corte`). O logout com token de sessão já revogada agora devolve `token_revogado`, mas continua provisório (QT-05, CT-09.04 desabilitado).

### 9.2 Mudanças

| Onde | O quê |
|------|-------|
| `erro/CodigoDeErro` | `TOKEN_AUSENTE` e `TOKEN_REVOGADO` |
| `erro/TratadorDeErros` | os dois novos → 401 |
| `sessao/ServicoDeSessao.validar` | ordem ausente → inválido → expirado (`!agora.isBefore(expiraEm)`) → revogado |
| `docs/arquitetura.md` | §4.2 (linha "Requisição") e ADR-014 |
| §2, §3, AM-01, QT-04, §7.4, §7.5 | atualizados neste plano |

### 9.3 Testes cujo resultado esperado mudou (com autorização do usuário)

Todas as mudanças trocam `token_invalido` por um código **mais específico**, no mesmo ponto da asserção. Nenhuma asserção foi removida ou afrouxada.

| Teste | Situação | Antes → depois |
|-------|----------|----------------|
| `SessaoTest#rn06_ct06_07_tokenDeSessaoRevogada` | validar token de sessão revogada | `TOKEN_INVALIDO` → `TOKEN_REVOGADO` |
| `SessaoTest#rn07_ct07_03_reusoForaDaToleranciaRevoga` | validar o token da sessão revogada pelo reuso | `TOKEN_INVALIDO` → `TOKEN_REVOGADO` |
| `SessaoTest#rn07_ct07_04_reusoDeOutroDispositivoRevoga` | idem | `TOKEN_INVALIDO` → `TOKEN_REVOGADO` |
| `SessaoTest#rn07_ct07_05_refreshMaisAntigoRevoga` | idem | `TOKEN_INVALIDO` → `TOKEN_REVOGADO` |
| `SessaoTest#rn09_ct09_01_logoutInvalidaTokenNaHora` | validar depois do logout | `TOKEN_INVALIDO` → `TOKEN_REVOGADO` |
| `SessaoTest#rn09_ct09_05_sessaoRevogadaNaoRessuscita` | idem | `TOKEN_INVALIDO` → `TOKEN_REVOGADO` |
| `SessaoTest#rn09_ct09_06_logoutConcorrenteComRenovacao` | validar o par renovado na corrida (a asserção da **renovação** continua `TOKEN_INVALIDO`) | `TOKEN_INVALIDO` → `TOKEN_REVOGADO` |
| `SessaoTest#rn09_ct09_07_logoutConcorrenteComRevogacao` | validar depois da revogação | `TOKEN_INVALIDO` → `TOKEN_REVOGADO` |
| `IdentidadeTest#rn04_ct04_01_recuperaERevogaSessoes` | validar token antigo depois da recuperação | `TOKEN_INVALIDO` → `TOKEN_REVOGADO` |
| `IdentidadeTest#rn04_ct04_08_recuperacaoRevogaSoDoTitular` | idem | `TOKEN_INVALIDO` → `TOKEN_REVOGADO` |
| `SessaoTest#rn06_ct06_09_*` | os valores nulo e vazio saíram deste teste e foram para o CT-06.10, com `TOKEN_AUSENTE` | método renomeado para `rn06_ct06_09_tokenIlegivel` |

**Testes novos ou desbloqueados:**
- **CT-06.03:** borda exata do TTL → `token_expirado`. Com CT-06.02 (−1 ms) e CT-06.04 (+1 ms), completa a trinca.
- **CT-06.08:** expirado e revogado → `token_expirado`.
- **CT-06.10:** ausente, com `@NullAndEmptySource` e espaços.
- **CT-06.11:** token expirado nega mesmo com premium vigente. O teste prova que a política liberaria o exclusivo, então a negação vem da expiração.
- **CT-21.01:** passa a cobrir os dois códigos novos (401).

**Cobertura:** em `ServicoDeSessao` ficaram 2 linhas sem cobertura:
- o logout com token de sessão já revogada, bloqueado por QT-05 (CT-09.04);
- o teste de nulo em `ler()`, que ficou defensivo, porque o token ausente agora é tratado antes.


## 10. Integração com o provedor de esportes: SportsProvider + WireMock (Aula 08)

> **Origem:** laboratório "Foot Fanatics" do material da Aula 08 (matriz INT-01 a INT-05; §5 "Cenários de degradação" e §6 "Segurança da chamada"). O provedor externo **não existe** em `docs/arquitetura.md`; ele entra aqui como requisito da disciplina e precisa de um ADR (ver §10.5).
> **Execução local:** `./mvnw -B clean verify`, em 2026-11-14: **BUILD SUCCESS**, 265 execuções, 0 falhas e 34 desabilitadas; cobertura de 96,6% das linhas e 88,6% dos branches. Só a classe do laboratório: `./mvnw -B test -Dtest=SportsProviderHttpTest` (27 execuções, ~5 s), repetida 3 vezes com o mesmo resultado.

### 10.1 O que foi construído

| Peça | Papel |
|------|-------|
| `esportes/SportsProvider` | **Porta**: `JogoExterno buscarJogo(long idExterno)`. Devolve o jogo validado ou lança `FalhaNaIntegracaoException` |
| `esportes/http/SportsProviderHttp` | **Adapter HTTP** (`java.net.http.HttpClient`, HTTP/1.1, sem redirecionamento): `GET {base}/matches/{id}` com `X-Api-Key` e `X-Correlation-Id` |
| `esportes/http/ConfiguracaoSportsProvider` | URL, token, timeouts e limite de tamanho vindos do ambiente (`SPORTS_PROVIDER_URL`, `SPORTS_PROVIDER_TOKEN`); `toString()` mascara o token |
| `esportes/ServicoDeJogos` | Só persiste **depois** de receber um jogo válido |
| `erro/TratadorDeErros` | Traduz a falha para Problem Details próprio, com `codigo` e `correlacao`, sem nada do provedor |

**Contrato declarado do provedor (hipótese nossa; o dublê prova a reação a ele, não o comportamento real do terceiro):** resposta 200 `application/json` com `{"id": inteiro, "status": "SCHEDULED"|"IN_PLAY"|"FINISHED", "home": inteiro ≥ 0, "away": inteiro ≥ 0}`.

### 10.2 Matriz de rastreabilidade (RTM): requisito → risco → teste → execução

Riscos da integração (novos, prefixo RK-INT):
- **RK-INT-01:** provedor lento ou fora do ar trava ou derruba o Foot Fanatics.
- **RK-INT-02:** dado externo inválido é gravado e corrompe o estado.
- **RK-INT-03:** credencial do provedor vaza (código, log, resposta).
- **RK-INT-04:** erro ou conteúdo do provedor é exposto ao usuário.
- **RK-INT-05:** repetir uma chamada condenada (4xx) agrava o problema (bloqueio por cota, credencial).

| Requisito (origem) | Risco | Teste (classe#método) | Asserções principais | Execução |
|--------------------|-------|-----------------------|----------------------|----------|
| **INT-01:** 200 válido → mapear o modelo interno (lab) | RK-INT-02 | `SportsProviderHttpTest#int01_partidaConcluidaMapeadaComRequestNoContrato`; `#int01_statusDoProvedorMapeado` (×3 status) | Objeto mapeado igual ao esperado **e** request com `X-Api-Key`, `X-Correlation-Id` e `Accept` (`verify` do WireMock) | ✅ local e CI (run 43547900481, §10.4) |
| **INT-02:** 404 → 404 próprio (lab) | RK-INT-04 | `#int02_jogoInexistenteVira404Proprio` | Motivo `NAO_ENCONTRADO`; Problem Details 404 `jogo_nao_encontrado`; corpo do provedor ausente; nada persistido | ✅ |
| **INT-03:** 500 → 503 controlado (lab) | RK-INT-01, RK-INT-04 | `#int03_erroDoProvedorVira503Controlado` (500, 502, 503); `#int03_provedorForaDoArVira503` (conexão recusada) | Motivo `INDISPONIVEL`; 503 `integracao_indisponivel`; **uma única** chamada; nada persistido | ✅ |
| **INT-04:** resposta lenta → interromper e não persistir (lab) | RK-INT-01, RK-INT-02 | `#int04_timeoutInterrompeENaoPersiste` | Motivo `INDISPONIVEL`; nada persistido; o cliente desiste antes do atraso de 5 s (read-timeout de 1,5 s, tolerância larga) | ✅ |
| **INT-05:** JSON inválido → rejeitar e registrar correlação (lab) | RK-INT-02 | `#int05_corpoInvalidoRejeitadoComCorrelacao` (10 casos: malformado, campo ausente, status desconhecido, tipo incompatível, placar fracionário, placar negativo, id divergente, `null`, content type não JSON, acima do limite) | Motivo `RESPOSTA_INVALIDA`; nada persistido; 502 `resposta_externa_invalida` com a **mesma** correlação do log e do header enviado | ✅ |
| **INT-06:** 4xx é permanente e não se repete (lab §5) | RK-INT-05 | `#int06_quatrocentosNaoSaoRepetidos` (400, 401, 403, 429) | Motivo `RECUSADA`; 503; **uma única** chamada | ✅ |
| **INT-07:** token do provedor no corpo de erro não atravessa a API (lab §6, "teste negativo obrigatório") | RK-INT-03, RK-INT-04 | `#int07_tokenDoProvedorNaoVaza`; `#int07_logSemCredencial`; `#configuracaoNuncaMostraOToken` | Token, mensagem e stack do provedor ausentes do Problem Details, da exceção (sem causa encadeada) e do log; log com correlação, status e duração; configuração imprime `token=***` | ✅ |
| Contrato de erro (RN-21) | RK-INT-04 | `ContratoDeErroTest#rn21_ct21_01_*` (+3 linhas) | `jogo_nao_encontrado` 404, `integracao_indisponivel` 503, `resposta_externa_invalida` 502 | ✅ |

### 10.3 Como os critérios de aceite do laboratório são garantidos

| Critério | Como |
|----------|------|
| Suíte offline e repetível | WireMock em porta aleatória de `127.0.0.1` (`dynamicPort()`); a "queda" usa uma porta local fechada. Nenhum teste aponta para host externo (busca por `http(s)://` em `src/test`: só loopback). 3 execuções seguidas com o mesmo resultado. |
| Nenhuma credencial real | Só `token-de-teste` nos testes; `application.properties` lê `${SPORTS_PROVIDER_URL}` e `${SPORTS_PROVIDER_TOKEN}`; `.env.example` com as variáveis vazias; INT-07 prova que o token não vai para log nem resposta |
| Cenários no pipeline | `SportsProviderHttpTest` roda no `./mvnw -B verify` do CI (Surefire), dentro do check exigido `verify (Java 21)`; relatórios nos artifacts `relatorios-de-teste-*` e `cobertura-jacoco-*` |
| RTM | Tabela §10.2 |
| README | Seção "Integração com o provedor de esportes" |

### 10.4 Execução no CI

| Run | Evento | Resultado | Evidência |
|-----|--------|-----------|-----------|
| [43547900481](https://github.com/GuilhermeAFSantos/foot-fanatics/actions/runs/43547900481) | PR #8, commit `5d6f17c` | ✅ `verify (Java 21)` | `SportsProviderHttpTest`: 27 execuções, 0 falhas, 4,6 s; suíte: 265 execuções, 0 falhas, 34 desabilitadas. `token-de-teste` aparece **0 vezes** no log do run. Artifacts `relatorios-de-teste-43547900481` e `cobertura-jacoco-43547900481` |
| SonarCloud, PR #8 | Análise do código novo | ✅ Quality Gate **Passed** | Cobertura do código novo 91,2%, duplicação 0%, 0 issues, 0 hotspots |

Os resultados ✅ da coluna "Execução" da §10.2 valem para a execução local e para este run.

### 10.5 Decisões provisórias e pendências

| Decisão | Por quê | Pendência |
|---------|---------|-----------|
| 502 `resposta_externa_invalida` para INT-05 | O material não fixa o status; 502 indica resposta inválida de um servidor upstream | Confirmar com o professor |
| Outro 4xx (inclusive 429) → 503 `integracao_indisponivel`, sem repetir | Do ponto de vista do usuário, o serviço está indisponível; o material manda não repetir 4xx | 429 poderia merecer espera (`Retry-After`) |
| Sem nova tentativa automática em 5xx | O material só permite retry "com limite, backoff e operação idempotente"; nada disso está definido | Política de retry |
| Limites: conexão 500 ms, leitura 1.500 ms, corpo 64 KiB | Valores do material (timeouts) e hipótese (tamanho) | Validar com o provedor |
| O read-timeout cobre a espera até os headers | Um provedor que mande os headers e "pingue" o corpo devagar não é interrompido pelo timeout | Timeout total de leitura |
| Sem wiring Spring (`@ConfigurationProperties`) | O projeto ainda não tem camada Spring (DG-01); ligar agora exigiria `SPORTS_PROVIDER_URL` para a aplicação subir | Junto com a AP-02 do ciclo 01 |
| Provedor fora de `docs/arquitetura.md` | Requisito vindo do laboratório | ADR do provedor externo (porta, contrato, degradação) |
