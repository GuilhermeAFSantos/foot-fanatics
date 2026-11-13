# Foot Fanatics

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

## Documentação

- [docs/prd.md](docs/prd.md): requisitos de produto
- [docs/arquitetura.md](docs/arquitetura.md): arquitetura
- [docs/plano-de-teste.md](docs/plano-de-teste.md): plano de teste
