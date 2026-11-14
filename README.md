# Foot Fanatics

[![CI](https://github.com/GuilhermeAFSantos/foot-fanatics/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/GuilhermeAFSantos/foot-fanatics/actions/workflows/ci.yml)

API REST de conteúdo esportivo (clubes, jogos, resultados e matérias) com assinatura: conta de usuário, controle de acesso e gestão de sessão. É o projeto do semestre da disciplina de qualidade.

## Escopos

| Escopo | Conteúdo |
|--------|----------|
| E1 Identidade & Conta | cadastro, credencial, perfil, recuperação de acesso |
| E2 Sessão & Acesso | login, emissão de token, expiração, renovação, logout |
| E3 Assinatura | planos free e premium, vigência, upgrade, cancelamento |
| E4 Conteúdo | clubes, jogos e matérias; parte pública, parte exclusiva de assinante |

**Fora de escopo:** pagamento real, app mobile, notificação push, painel administrativo.

## Stack

Java 21 · Spring Boot 4.1.2 · Spring Web, Security, Data JPA e Validation · PostgreSQL · Maven (wrapper) · Testcontainers

## Pré-requisitos

| Ferramenta | Para quê |
|------------|----------|
| **Java 21** (JDK) | compilar e rodar a API |
| **Docker** em execução | os testes e o `spring-boot:test-run` sobem um PostgreSQL via Testcontainers |
| **Node.js 18+** | AIOX (`.aiox-core/`), framework de agentes usado no desenvolvimento |

O Maven não precisa estar instalado: o projeto traz o wrapper (`mvnw` / `mvnw.cmd`).

> **Windows:** o Docker Desktop exige WSL 2. Para isso, a virtualização (Intel VT-x / AMD-V) precisa estar habilitada na BIOS/UEFI, e o recurso *Virtual Machine Platform* precisa estar ativo no Windows.

## Como subir

Sem PostgreSQL local, use o modo de desenvolvimento, que sobe um PostgreSQL descartável em container:

```bash
./mvnw spring-boot:test-run        # Windows (cmd/PowerShell): mvnw.cmd spring-boot:test-run
```

A API fica em `http://localhost:8080`. Por enquanto todas as rotas exigem autenticação (padrão do Spring Security) e respondem `401`.

Com um PostgreSQL próprio, configure `spring.datasource.*` em `src/main/resources/application.properties` e rode:

```bash
./mvnw spring-boot:run
```

## Como rodar os testes

Com o Docker em execução:

```bash
./mvnw test                        # Windows: mvnw.cmd test
```

Os relatórios ficam em `target/surefire-reports/`.

## Integração contínua

O workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) roda em todo **pull request** e em **push na `main`**:

- JDK 21 (Temurin) com cache do Maven, em `ubuntu-24.04`, com timeout de 20 minutos.
- O mesmo `./mvnw -B verify` usado localmente, incluindo os testes com Testcontainers. Nenhum teste é pulado.
- **Cobertura (JaCoCo 0.8.15):** o `verify` falha abaixo de 80% de linhas ou 70% de branches. O relatório (`target/site/jacoco/`) é publicado como artifact `cobertura-jacoco-<run_id>`.
- **Análise estática (SonarCloud):** depois do `verify`, o plugin Maven analisa o projeto [`GuilhermeAFSantos_foot-fanatics`](https://sonarcloud.io/project/overview?id=GuilhermeAFSantos_foot-fanatics) lendo o XML do JaCoCo e espera o Quality Gate (`sonar.qualitygate.wait=true`). Gate reprovado = check vermelho.
- `permissions: contents: read`, sem `continue-on-error` e sem `pull_request_target`. O único secret é `SONAR_TOKEN`, usado só no step do SonarCloud e mascarado nos logs. PRs de fork não recebem o secret e pulam esse step.
- Execuções obsoletas do mesmo PR ou branch são canceladas (`concurrency`).
- Os relatórios do Surefire e do Failsafe são publicados como artifact em toda execução (`if: always()`), com retenção de 7 dias.

Para medir a cobertura localmente, use `./mvnw -B clean verify`: sem `clean`, o JaCoCo soma os dados da execução anterior.

**Proteção da `main`:** só aceita alterações com os checks **`verify (Java 21)`** e **`SonarCloud Code Analysis`** verdes e a branch atualizada em relação à `main`. Exigir o check do SonarCloud garante o Quality Gate também em PRs de fork, onde o step do CI é pulado. A regra vale também para administradores, e force push e exclusão da branch estão bloqueados.

### Validação do pipeline (PR #1, 2026-11-13)

| Run | Commit | Resultado | O que demonstra |
|-----|--------|-----------|-----------------|
| [43511428888](https://github.com/GuilhermeAFSantos/foot-fanatics/actions/runs/43511428888) | `4f4a0c5` | ✅ success | Check verde no PR: 199 testes, 0 falhas, 36 desabilitados |
| [43511699150](https://github.com/GuilhermeAFSantos/foot-fanatics/actions/runs/43511699150) | `92e86e4` | ❌ failure | Asserção quebrada de propósito (`ContratoDeErroTest`, CT-21.01: `expected: <402> but was: <403>`) |
| [43512113294](https://github.com/GuilhermeAFSantos/foot-fanatics/actions/runs/43512113294) | `be19db8` | ✅ success | Revert da quebra: verde de novo, 199 testes, 0 falhas |

## Documentação

- [docs/prd.md](docs/prd.md): requisitos de produto
- [docs/arquitetura.md](docs/arquitetura.md): arquitetura
- [docs/plano-de-teste.md](docs/plano-de-teste.md): plano de teste
