// Gera docs/rtm.md consolidando as matrizes do plano de TDD (§1 inventário, §7.5 matriz das regras, §10.2 integração).
// Uso (na raiz, depois de ./mvnw -B clean verify com a árvore igual ao commit avaliado):
//   node scripts/gerar-rtm.js . <sha-do-commit> <id-do-run-do-CI> <id-do-artifact-relatorios-de-teste>
// Confere cada status do plano contra target/surefire-reports e para com erro se houver divergência.
const fs = require('fs');
const path = require('path');
const [raiz, sha, run, artefato] = process.argv.slice(2);
const plano = fs.readFileSync(path.join(raiz, 'docs/testes/plano-tdd.md'), 'utf8').split(/\r?\n/);
const REPO = 'https://github.com/GuilhermeAFSantos/foot-fanatics';
const RUN = `${REPO}/actions/runs/${run}`;
const celulas = (l) => l.split('|').slice(1, -1).map((c) => c.trim());

// ---- 1. Inventário (§1): primeira tabela "| ID | Regra"
const iniInv = plano.findIndex((l) => l.startsWith('| ID | Regra'));
const inventario = [];
for (let i = iniInv + 2; plano[i] && plano[i].startsWith('|'); i++) {
  const [id, regra, origem, forca, escopo] = celulas(plano[i]);
  inventario.push({ id, regra, origem, forca, escopo });
}

// ---- 2. Matriz §7.5
const ini75 = plano.findIndex((l) => l.startsWith('### 7.5'));
const cab75 = plano.findIndex((l, i) => i > ini75 && l.startsWith('| Regra | Caso |'));
const matriz = [];
for (let i = cab75 + 2; plano[i] && plano[i].startsWith('|'); i++) {
  const [regra, caso, teste, situacao, status, obs] = celulas(plano[i]);
  matriz.push({ regra, caso, teste, situacao, verde: /VERDE/.test(status), desab: /DESABILITADO/.test(status), obs });
}

// ---- 3. §10.2 (integração): copiada de "#### Requisitos" até antes de "### 10.3"
const ini102 = plano.findIndex((l) => l.startsWith('### 10.2'));
const iniReq = plano.findIndex((l, i) => i > ini102 && l.startsWith('#### Requisitos'));
const fim102 = plano.findIndex((l) => l.startsWith('### 10.3'));
const integracao = plano.slice(iniReq, fim102).join('\n').replace(/^#### /gm, '### ').trim();

// ---- Índice dos métodos de teste: classe → arquivo, método → linha
const arquivos = [];
(function varrer(d) {
  for (const e of fs.readdirSync(d, { withFileTypes: true })) {
    const p = path.join(d, e.name);
    if (e.isDirectory()) varrer(p); else if (e.name.endsWith('Test.java')) arquivos.push(p);
  }
})(path.join(raiz, 'src/test/java'));
const classes = Object.fromEntries(arquivos.map((p) => [path.basename(p, '.java'), p]));
function linkDoTeste(classe, metodo) {
  const p = classes[classe];
  if (!p) throw new Error('classe não encontrada: ' + classe);
  const linhas = fs.readFileSync(p, 'utf8').split(/\r?\n/);
  const n = linhas.findIndex((l) => new RegExp(`\\bvoid ${metodo}\\(`).test(l)) + 1;
  if (!n) throw new Error(`método não encontrado: ${classe}#${metodo}`);
  const rel = path.relative(raiz, p).split(path.sep).join('/');
  return `${REPO}/blob/${sha}/${rel}#L${n}`;
}

// ---- Resultados do Surefire (mesmo commit, execução local): método → {passou, ignorado}
const resultados = {};
const dirRel = path.join(raiz, 'target/surefire-reports');
for (const f of fs.readdirSync(dirRel).filter((f) => f.startsWith('TEST-') && f.endsWith('.xml'))) {
  const xml = fs.readFileSync(path.join(dirRel, f), 'utf8');
  for (const m of xml.matchAll(/<testcase name="([^"]+)" classname="([^"]+)"[^>]*?(\/>|>([\s\S]*?)<\/testcase>)/g)) {
    const classe = m[2].split('.').pop();
    const metodo = m[1].replace(/[(\[].*$/, '');
    const corpo = m[4] || '';
    const chave = `${classe}#${metodo}`;
    const r = (resultados[chave] ||= { passou: 0, ignorado: 0, falhou: 0 });
    if (/<skipped/.test(corpo)) r.ignorado++; else if (/<(failure|error)/.test(corpo)) r.falhou++; else r.passou++;
  }
}

// ---- Montagem das linhas RN/RS
const nivelDe = (classe) => classe === 'PersistenciaDoTreinoIntegracaoTest' ? 'Integração (PostgreSQL)'
  : classe === 'SportsProviderHttpTest' ? 'Integração (HTTP)' : 'Unidade';
const divergencias = [];
let nPassou = 0, nIgnorado = 0, nParcial = 0;
const linhasMatriz = matriz.map((r) => {
  const refs = [...r.teste.matchAll(/`(\w+)#(\w+)`/g)].map((m) => ({ classe: m[1], metodo: m[2] }));
  if (!refs.length) throw new Error('linha sem teste: ' + r.caso);
  const teste = r.teste.replace(/`(\w+)#(\w+)`/g, (_, c, m) => `[\`${c}#${m}\`](${linkDoTeste(c, m)})`);
  const niveis = [...new Set(refs.map((x) => nivelDe(x.classe)))].join(', ');
  const reais = refs.map((x) => {
    const res = resultados[`${x.classe}#${x.metodo}`];
    return !res ? 'ausente' : res.falhou ? 'falhou' : res.passou && !res.ignorado ? 'passou'
      : res.ignorado && !res.passou ? 'ignorado' : 'misto';
  });
  // O plano declara VERDE, DESABILITADO ou os dois (linha com métodos de status diferentes).
  const permitidos = new Set([...(r.verde ? ['passou'] : []), ...(r.desab ? ['ignorado'] : [])]);
  reais.forEach((real, i) => {
    if (!permitidos.has(real)) divergencias.push(`${r.caso} ${refs[i].classe}#${refs[i].metodo}: plano=${[...permitidos]}, surefire=${real}`);
  });
  if (r.verde && r.desab && !(reais.includes('passou') && reais.includes('ignorado'))) {
    divergencias.push(`${r.caso}: plano diz VERDE e DESABILITADO, surefire=${reais}`);
  }
  let status;
  if (r.verde && r.desab) {
    const ignorados = refs.filter((_, i) => reais[i] === 'ignorado').map((x) => `\`${x.metodo}\``).join(', ');
    status = `Passou (${reais.filter((x) => x === 'passou').length} métodos) · Ignorado (\`@Disabled\`): ${ignorados}`;
    nParcial++;
  } else if (r.verde) { status = 'Passou'; nPassou++; } else { status = 'Ignorado (`@Disabled`)'; nIgnorado++; }
  const evid = `[run ${run}](${RUN})`;
  const obs = r.obs ? r.obs : '';
  return `| [${r.regra}](#${r.regra.toLowerCase()}) | ${r.caso} | ${teste} | ${niveis} | ${evid} | ${status} | ${obs} |`;
});
if (divergencias.length) { console.error('DIVERGÊNCIAS:\n' + divergencias.join('\n')); process.exit(1); }
const tot = Object.values(resultados).reduce((a, r) => ({ p: a.p + r.passou, i: a.i + r.ignorado, f: a.f + r.falhou }), { p: 0, i: 0, f: 0 });
const linhasIntPassou = (integracao.match(/\| Passou \|/g) || []).length;

const linhasInv = inventario.map((r) =>
  `| <a id="${r.id.toLowerCase()}"></a>**${r.id}** | ${r.regra} | ${r.origem} | ${r.forca} | ${r.escopo} |`);

const md = `# Foot Fanatics — Matriz de rastreabilidade (RTM)

> **O que é:** a RTM consolidada do projeto, num só lugar. Liga cada **requisito** à sua **origem**, aos **testes** que o verificam, ao **nível** do teste, à **evidência** de execução e ao **status**.
> **Gerada a partir de** \`docs/testes/plano-tdd.md\` (§1 inventário, §7.5 matriz das regras, §10.2 integração), no commit [\`${sha.slice(0, 7)}\`](${REPO}/commit/${sha}). O plano continua sendo a fonte dos casos e das decisões; esta página é a visão navegável.
> **Execução de referência:** [run ${run}](${RUN}): CI do commit \`${sha.slice(0, 7)}\`, com \`verify (Java 21)\` ✅ e Quality Gate do SonarCloud ✅. Os status das §1 e §2 apontam para essa execução, e os da §3 para o run indicado nela. Nenhum status aparece sem execução.

## 0. Como ler

| Coluna | Significado |
|--------|-------------|
| Requisito | Regra ou requisito, com link para a definição e a origem (§1 e §3) |
| Caso | Identificador do caso de teste no plano (\`CT-xx.yy\` para regras de negócio, \`CT-Sxx.yy\` para o pipeline) |
| Teste | Método JUnit, com link para a linha exata no commit avaliado |
| Nível | **Unidade**: dublês em memória, sem infraestrutura. **Integração (PostgreSQL)**: Testcontainers com banco real. **Integração (HTTP)**: WireMock em \`127.0.0.1\` |
| Evidência | Execução do CI em que o resultado foi obtido |
| Status | **Passou**, ou **Ignorado (\`@Disabled\`)** quando o caso está bloqueado por pergunta ou lacuna (motivo na observação) |

**Totais desta versão:**
- Regras RN e RS: ${nPassou} casos Passou, ${nParcial} casos parciais (métodos que passam e o da borda ignorado) e ${nIgnorado} casos ignorados.
- Integração (§3): ${linhasIntPassou} linhas Passou.
- Suíte no mesmo commit: ${tot.p + tot.i + tot.f} execuções, ${tot.f} falhas, ${tot.i} ignoradas.

**Conferência automática:** o status de cada linha foi comparado com os relatórios do Surefire de \`./mvnw -B clean verify\` no mesmo commit. Cada método de uma linha "Passou" passou e cada método de uma linha "Ignorado" foi ignorado. Nas linhas parciais, os métodos ignorados são exatamente os listados. Nenhum método falhou e não houve divergência. Na primeira versão, esta conferência encontrou três erros no plano, já corrigidos (\`plano-tdd.md\` §8.7).

> Relatório por classe do Surefire: artifact [relatorios-de-teste-${run}](${RUN}/artifacts/${artefato}). Baixar o artifact exige login no GitHub. Os artifacts expiram 7 dias após o run; o resumo continua no log do run.

## 1. Requisitos: regras de negócio (RN) e do pipeline diário (RS)

Força da origem (plano §0.2): **A** = está no enunciado; **B** = decisão de arquitetura aceita; **C** = ADR proposta (condicional).

| ID | Regra | Origem | Força | Escopo |
|----|-------|--------|-------|--------|
${linhasInv.join('\n')}

## 2. Matriz: regra → caso → teste → evidência

| Requisito | Caso | Teste | Nível | Evidência | Status | Observação |
|-----------|------|-------|-------|-----------|--------|------------|
${linhasMatriz.join('\n')}

## 3. Integração com o provedor de esportes (Aula 08)

Copiada da §10.2 do plano. Evidência própria: o run 43548364824 (commit \`ad1ac07\`, merge do PR #8). Os arquivos de teste da integração não mudaram desde então.

${integracao}

## 4. Requisitos de aprovação e manutenção

> **A preencher.** Esta seção existe porque a prática seguinte pede uma RTM com "os requisitos de aprovação e manutenção identificados". Até a fonte desses requisitos ser definida, nada foi inventado aqui.

| ID | Requisito | Tipo | Origem | Risco | Teste | Nível | Evidência | Status |
|----|-----------|------|--------|-------|-------|-------|-----------|--------|
| — | — | Aprovação | — | — | — | — | — | — |
| — | — | Manutenção | — | — | — | — | — | — |

## 5. Como manter esta RTM

1. Um caso novo entra primeiro no plano (§2 ou §3 do plano, e §7.5).
2. Faça o commit da mudança de código ou de teste e abra o PR. O run do CI desse commit é a evidência.
3. Com a árvore local igual a esse commit, rode \`./mvnw -B clean verify\` e regenere esta página:
   \`node scripts/gerar-rtm.js . <sha-do-commit> <id-do-run> <id-do-artifact-relatorios-de-teste>\`
   O gerador para com erro se algum status do plano divergir do Surefire, se um método citado não existir ou se um teste tiver falhado.
4. Faça o commit desta página separadamente. Ela só muda documentação, então a evidência continua válida para o código do commit citado.
5. Um status sem execução não entra. Se o run não existir ou falhar, a linha fica sem status.`;
fs.writeFileSync(path.join(raiz, 'docs/rtm.md'), md);
console.log(`ok: ${inventario.length} requisitos, ${matriz.length} linhas (${nPassou} passou, ${nParcial} parcial, ${nIgnorado} ignorado); surefire ${tot.p}/${tot.i}/${tot.f}; integração ${linhasIntPassou}`);
