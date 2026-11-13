# Foot Fanatics — PRD
## 1. Visão do produto
TODO
## 2. Problema
TODO
## 3. Personas

> **Origem:** personas geradas com apoio de IA em 2026-11-13, a partir do enunciado do projeto (API de conteúdo esportivo com assinatura; temas de tempo, risco, ramificação e estado compartilhado). **Não** são as personas escritas na aula de ATAM. **Validação pendente** com o professor/turma. Nenhum número de uso ou volume foi atribuído a elas.

### P1 — Rafa, torcedor visitante (sem conta ou plano free)
- **Contexto:** acompanha o clube pelo celular e pelo navegador, consumindo uma aplicação cliente que usa a API. Lê resultados e matérias abertas, sem conta ou com conta no plano free.
- **Objetivo:** ver rapidamente jogos, resultados e notícias do clube e entender o que ganharia assinando o premium antes de decidir pagar.
- **Frustrações:** não saber de antemão o que é conteúdo pago; abrir uma matéria e só depois descobrir que está bloqueada; perder o progresso (ou a matéria que queria ler) ao ser mandado para cadastro/login.
- **Temas de qualidade que exercita:** *ramificação* (anônimo × free × premium sobre o mesmo conteúdo) e transição free → premium (upgrade).

### P2 — Bia, assinante premium
- **Contexto:** assinante do plano premium, usa a conta em mais de um dispositivo ao mesmo tempo (ex.: celular e computador) e consome o conteúdo exclusivo de assinante.
- **Objetivo:** acessar o conteúdo exclusivo sem interrupção durante toda a vigência que pagou, em qualquer dispositivo, e conseguir cancelar ou recuperar o acesso sozinha.
- **Frustrações:** ser deslogada no meio de uma leitura porque o token expirou sem renovação; perder acesso premium antes do fim da vigência (ou manter acesso depois de cancelar, sem entender por quê); logout em um dispositivo derrubar ou não derrubar os outros de forma imprevisível; não conseguir recuperar a conta ao esquecer a senha.
- **Temas de qualidade que exercita:** *tempo* (expiração e renovação de token, vigência do plano), *estado compartilhado* (sessões simultâneas sob concorrência) e *risco* (cancelamento e vencimento precisam cortar o acesso no momento certo).

### P3 — Caio, desenvolvedor que integra a API
- **Contexto:** desenvolve a aplicação cliente (web) que consome a API Foot Fanatics. Não tem acesso ao código do servidor, só ao contrato da API e às respostas que ela devolve.
- **Objetivo:** integrar cadastro, login, renovação de sessão e consulta de conteúdo de forma previsível, distinguindo nas respostas quem não está autenticado de quem está autenticado mas sem permissão.
- **Frustrações:** respostas de erro inconsistentes (mesma situação devolvendo códigos diferentes); não conseguir distinguir "token expirado" de "token inválido" para decidir se renova ou manda ao login; a API devolver dados de conteúdo premium (ou de outro usuário) para quem não deveria recebê-los — o que vira vazamento em produção, não um bug visual.
- **Temas de qualidade que exercita:** *risco* (falha de autorização = vazamento de dados), *contrato* previsível de erros e o ciclo de vida do token (emissão, expiração, renovação, logout) do ponto de vista de quem consome.
## 4. Escopos
- E1 Identidade & Conta
- E2 Sessão & Acesso
- E3 Assinatura
- E4 Conteúdo
## 5. Fora de escopo
- Pagamento real, mobile, push, painel administrativo
## 6. Métricas de sucesso
TODO
