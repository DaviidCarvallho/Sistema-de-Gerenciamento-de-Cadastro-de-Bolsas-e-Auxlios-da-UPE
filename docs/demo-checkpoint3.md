# Roteiro de demonstração — Checkpoint 3 (SGBA Back-end)

> Execução completa em **< 10 minutos** com a coleção **Insomnia** (`docs/insomnia/sgba-checkpoint3.insomnia.json`)
> ou a coleção **Postman** equivalente (`docs/postman/sgba-checkpoint3.postman_collection.json`).
> Cada request tem na **descrição** o status esperado e o código de erro D2 (`{status, erro, mensagem}`).

**Banco limpo antes de começar** (os ids do seed precisam bater com o environment `dev`):

```bash
docker compose down -v
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

No log, procure `[DevSeedData] Seed aplicado` e confira os ids: **aluno=1 · professor=2 · gerenciador=3 · programa=1 · editais 1–4** (A=1/08-2026 aberto · B=2/09-2026 exigências obrigatórias · C=3/07-2025 encerrado · D=4/10-2026 sem formulário).

**Importar a coleção:** Insomnia → *Import* → *File* → `docs/insomnia/sgba-checkpoint3.insomnia.json`. O environment `dev (compose + seed)` já vem populado.

---

## Bloco 1 — Fundação e infraestrutura (C5 base) · 1 min

| # | Request | Prova |
|---|---------|-------|
| 1 | **0 · Fundação** → `GET /api/inexistente` | **404** com body `{status, erro, mensagem}` — handler base (D2) |
| 2 | Papel: PG do Docker (`docker compose ps` = `healthy`) e o log do seed | RNF01/RNF04 |

## Bloco 2 — Usuários e Alunos (Issue 2) · 1 min

| # | Request | Prova |
|---|---------|-------|
| 1 | **1 · Usuários** → `POST` e-mail `@gmail.com` | **400** `EMAIL_INSTITUCIONAL_INVALIDO` (RN01) |
| 2 | `POST` e-mail duplicado (`aluno@upe.br`) | **409** `EMAIL_JA_CADASTRADO` |
| 3 | `GET /usuarios?funcao=PROFESSOR` | **200** lista (base do RN10/wizard) |
| 4 | **2 · Alunos** → `GET /{usuarioAlunoId}` (perfil do seed) | **200** perfil 1:1 via `MapsId` (CRA 8.50, curso...) |

## Bloco 3 — Submissões: ciclo de vida (Issue 5 — módulo central) · 4 min

Todos no header `X-Usuario-Id` (D1 — sem auth no cp3).

| # | Request (pasta **5 · Submissões**) | Prova |
|---|-------------------------------------|-------|
| A1 | `POST /api/submissoes` rascunho no Edital A | **201** + `Location`; nasce **RASCUNHO** sem protocolo (DER v3) |
| A2 | repetir A1 (mesmo aluno+edital) | **409** `SUBMISSAO_DUPLICADA` (**RN08**) |
| A3 | `orientadorId` = aluno | **400** `ORIENTADOR_INVALIDO` (**RN10**) |
| A4 | `POST /enviar` sem responder/termo | **409** `ENVIO_INCOMPLETO` com a **lista de pendências** (**RN12**) |
| A5 | `PUT` respostas+`termoAceite:true` | **200** (upsert por `uk_resposta_unica`) |
| A6 | `POST /enviar` | **200** `RECEBIDA` + protocolo **`#2026-0001`** (**RN02/RN05**) + snapshot (**RN09**) — anote `submissaoIdA` |
| B  | rascunho no Edital C (janela 2025 encerrada) → `enviar` | **409** `FORA_DO_PRAZO` (**RN05**) |
| C  | rascunho no Edital B (exigências obrigatórias) → `enviar` | **409** `ENVIO_INCOMPLETO` **listando os 2 documentos** — segue bloqueado até a pasta Documentos |
| D1–D7 | `PATCH /status` pelo **gerenciador** (id 3): `EM_ANALISE` → `PENDENTE_DOCUMENTOS` (motivo) → *(após Documentos)* `EM_ANALISE` → `APROVADA` | Transições da **tabela RF16**; sem motivo → **400** `MOTIVO_OBRIGATORIO` (**RN04**); com header de aluno → **400** `PERMISSAO_INSUFICIENTE` (§9.2); `RASCUNHO→APROVADA` → **409** `TRANSICAO_INVALIDA` |
| E3 | `GET /{submissaoIdA}/historico` | **200** timeline com os **5 eventos** da sequência D1–D7 (envio + 4 PATCHs: statusAnterior/Novo, observação, dataHora, usuário) — **RF12/RN07** (a variante de 4 eventos corresponde a parar em APROVADA logo após a pendência) |
| E5 | `GET /api/gerenciador/submissoes?status=` | **200** painel do gerenciador (**RF14**) |
| F1–F2 | `PUT /api/alunos/{id}` muda `craAtual` → `GET /{submissaoIdA}` | `craMomento` **continua 8.50** — snapshot imutável do mérito (**RN09**) |

## Bloco 4 — Documentos (Issue 6) · 2 min

Sequência recomendada: deixe o rascunho do **Edital B** ainda em `RASCUNHO` (não envoie antes da pasta 6).

| # | Request (pasta **6 · Documentos**) | Prova |
|---|-------------------------------------|-------|
| D1 | `POST /{submissaoBId}/documentos` multipart `arquivo` + `tipoDocumento=HISTORICO_ESCOLAR` (PDF local) | **201** com `statusValidacao=PENDENTE` (**RF09/D7**) — anote `documentoId` |
| D2 | mesmo request com arquivo `.txt` | **400** `ARQUIVO_INVALIDO` |
| D3 | `tipoDocumento=OUTRO` sem `descricaoPersonalizada` | **400** `DESCRICAO_OBRIGATORIA` (**RN11**) |
| D4 | `COMPROVANTE_MATRICULA` (PDF) | **201** — agora `POST /{submissaoBId}/enviar` sobre a submissão desbloqueia (**RN12** completude) |
| D6 | `GET /api/documentos/{documentoId}/download` | **200** `application/pdf`, bytes idênticos ao upload |
| D8 | `PATCH /api/documentos/{documentoId}/status` VALIDO (gerenciador) | **200** — validação do anexo no painel (**RF15**) |
| D9 | idem com header de aluno | **400** `PERMISSAO_INSUFICIENTE` (§9.2) |

Na sequência, volte à pasta 5 para `D6: PENDENTE_DOCUMENTOS → EM_ANALISE → APROVADA` e feche o histórico de 4 eventos.

## Bloco 5 — Notificações (Issue 6) · 1 min

| # | Request (pasta **7 · Notificações**) | Prova |
|---|---------------------------------------|-------|
| N1 | `GET /api/notificacoes` (header do aluno) | **200** — inbox com 1 notificação **por transição de status** (hook conectado na mesma transação — RF17) |
| N2 | `?somenteNaoLidas=true` | **200** — filtra |
| N3–N4 | `PATCH /{notificacaoId}/lida` (duas vezes) | **200** — idempotente (decisão do PR) |
| N5 | `PATCH` com header de outro usuário | **404** — não vazar inbox alheio |

## Bloco 6 — Fechamento (C1–C5 em uma frase cada)

- **C1:** entidades JPA 1:1 com o DER v3 (auditoria no PR da Issue 1 — `docs/auditoria-entidades-der.md`); `submissoes.protocolo` nasce null e é preenchido no envio (prova no bloco 3).
- **C2:** **11 repositories** Spring Data com consultas de negócio (uniqs, janelas, painéis — C2).
- **C3:** as RNs listadas por módulo moram **nos Services** (nenhum `if` de negócio nos controllers) — demose acima mostrou cada código de erro do DRS.
- **C4:** 201+`Location` · 200 · 204 · 400/404/409 coerentes via `GlobalExceptionHandler` base.
- **C5:** esta coleção (Insomnia/Postman) é a evidência; roteiro cobre todos os 5 critérios em < 10 min.

> **Testes automatizados como reforço (D8):** `./mvnw test` roda a suite com H2 — inclui os fluxos de usuário/aluno, editais, submissões (RN08/RN10/RN03/RN05/RN09/RN12/RF16/RN04/§9.2), documentos e notificações.
