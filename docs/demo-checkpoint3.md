# Roteiro de demonstração — Checkpoint 3 (SGBA Back-end) · CRUD essencial via Insomnia

> Fonte: coleção **`docs/insomnia/sgba-essencial.insomnia.json`** — a lista do **CRUD essencial** acordada para a arguição: **7 módulos · 23 requests**, demonstração completa em **< 10 minutos**.
> A coleção completa (`docs/insomnia/sgba-checkpoint3.insomnia.json`, 85 requests) fica de **plano B**: provas de erro (códigos D2) e casos especiais — ver bloco final.
> Erros seguem o formato D2: `{ "status": int, "erro": "CODIGO", "mensagem": "..." }`.

**Banco limpo antes de começar** (os ids do seed precisam bater com o environment `dev (seed)`):

```bash
docker compose down -v
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

No log, procure `[DevSeedData] Seed aplicado` e confira os ids: **aluno=1 · professor=2 · gerenciador=3 · programa=1 (PIBIC) · editais 1–4** (A=1/08-2026 aberto · B=2/09-2026 exigências obrigatórias · C=3/07-2025 encerrado · D=4/10-2026 sem formulário).

**Importar:** Insomnia → *Import* → *File* → `docs/insomnia/sgba-essencial.insomnia.json`. O environment **`dev (seed)`** já vem populado (`baseUrl=http://localhost:8080`, `alunoId=1`, `professorId=2`, `gerenciadorId=3`, `programaId=1`, `editalAId=1`, `perguntaAId=1`, `submissaoId=1`, `documentoId=1`, `notificacaoId=1`).

> **Ordem importa:** rode os módulos 1→7 na sequência das pastas. A criação do usuário no bloco 1 gera o `usuarioId=4` que o bloco 2 usa; o envio do bloco 5 gera a notificação que o bloco 7 lista.

---

## Bloco 1 — Usuários (R · R · C · U) · 1 min

Pasta **Usuários**.

| # | Request | Demonstração |
|---|---------|--------------|
| 1 | `GET /api/usuarios?funcao=PROFESSOR` | **200** — lista/filtro por papel (retorna o professor do seed, id 2) |
| 2 | `GET /api/usuarios/{alunoId}` (id 1) | **200** — detalhe: `nome`, `email` institucional, `funcao=ALUNO` |
| 3 | `POST /api/usuarios` — `novo@upe.br`, `funcao=ALUNO` | **201** + `Location` — **anote: usuário criado com id 4** |
| 4 | `PUT /api/usuarios/{alunoId}` (id 1) — body só `{"nome": ...}` | **200** — update **parcial**: só `nome` muda; `email`/`funcao` intactos (`atualizadoEm` preenchido) |

*Fala:* tabela `usuarios` guarda a identidade (nome, e-mail, senha hash, papel); e-mail institucional e filtro por papel saem de consultas de negócio no repository.

## Bloco 2 — Alunos (R · C) · 1 min

Pasta **Alunos**. Perfil acadêmico **1:1 com `MapsId`** — a tabela `alunos` **não tem** coluna nome/e-mail: pega emprestada a PK do usuário.

| # | Request | Demonstração |
|---|---------|--------------|
| 1 | `GET /api/alunos/{alunoId}` (id 1, seed) | **200** — perfil: `cpf`, `matricula`, `curso`, **`craAtual=8.50`** |
| 2 | `POST /api/alunos` — body com `usuarioId=4` | **201** — **prova do `MapsId`: o id do aluno é o mesmo do usuário (4)**; duplicar o request → **409** `ALUNO_JA_EXISTENTE` (1 perfil por usuário) |

*Fala:* sem nome/e-mail no cadastro de aluno porque já são do usuário — normalização, uma PK compartilhada (`alunos.usuario_id` é PK **e** FK).

## Bloco 3 — Programas de Bolsa (R · R · R · C · U) · 1,5 min

Pasta **Programas de Bolsa**. Catálogo com **visibilidade por janela de datas** (RF05).

| # | Request | Demonstração |
|---|---------|--------------|
| 1 | `GET /api/programas?page=0&size=10` | **200** — listagem **paginada** (`Page`, metadados de paginação no JSON) |
| 2 | `GET /api/programas/abertos` | **200** — vitrine pública: só o PIBIC do seed está na janela; **rode de novo após o nº 4** — a Monitoria (janela 01/11–31/12/2026) entra na lista |
| 3 | `GET /api/programas/{programaId}` (id 1) | **200** — detalhe com `valorMensal`, `vagas`, datas |
| 4 | `POST /api/programas` — "Monitoria de Algoritmos" | **201** + `Location` — **id 2** |
| 5 | `PUT /api/programas/{programaId}` (id 1) | **200** — update completo |

*Fala:* regra de janela (RF05) mora no **Service/Repository** — `/abertos` filtra por `dataAbertura ≤ agora ≤ dataLimite` (provada também na coleção completa com 400 `PERIODO_INVALIDO`).

## Bloco 4 — Editais (R · R · C) · 1 min

Pasta **Editais**.

| # | Request | Demonstração |
|---|---------|--------------|
| 1 | `GET /api/editais/{editalAId}` (id 1) | **200** — Edital A: nº/ano, janela **aberta**, link com o programa |
| 2 | `GET /api/editais/{editalAId}/formulario` (id 1) | **200** — **pergunta ordenada** + exigências do edital (é justamente o que o envio valida) |
| 3 | `POST /api/editais` — nº 11/2026, janela aberta | **201** + `Location` — **edital id 5** |

*Fala:* unicidade nº+ano (uk do DER) e janela invertida → 400/409 — provas de erro na coleção completa (`EDITAL_DUPLICADO`, `JANELA_INVALIDA`).

## Bloco 5 — Submissões (R · C · R · U · finalizar) · 2 min · módulo central

Pasta **Submissões**. Todos os requests exigem o header **`X-Usuario-Id: 1`** (aluno — D1).

| # | Request | Demonstração |
|---|---------|--------------|
| 1 | `GET /api/submissoes/minhas` | **200** `[]` — lista do aluno **nunca é null** |
| 2 | `POST /api/submissoes` (rascunho, Edital A, `orientadorId=2` — professor) | **201** + `Location` — nasce **`RASCUNHO`**, **sem protocolo** (protocolo só no envio), header do aluno |
| 3 | `GET /api/submissoes/{submissaoId}` (id 1 — rode duas vezes: no rascunho e após enviar) | **200** — detalhe: status, respostas, snapshot (null em rascunho) |
| 4 | `PUT /api/submissoes/{submissaoId}` — respostas (pergunta 1) + `termoAceite=true` | **200** — upsert por `perguntaId` (idempotente: repetir o PUT não duplica) |
| 5 | `POST /api/submissoes/{submissaoId}/enviar` | **200** — status **`RECEBIDA`** + protocolo **`#2026-0001`** (RN02, ano corrente) + **snapshot do mérito** CRA 8.50 (RN09) → **também cria a notificação** (RF17 — hook na mesma transação) |

*Fala:* ciclo de vida completo num só módulo: rascunho → completar → enviar. O protocolo nasce null e é preenchido só no `enviar` (a prova é comparar o GET do nº 3 antes/depois). Enviar sem a resposta/termo → **409** `ENVIO_INCOMPLETO` (coleção completa).

## Bloco 6 — Documentos (C · R) · 1 min

Pasta **Documentos**. Header **`X-Usuario-Id: 1`** (dono do rascunho). Precisa de **um PDF local** (qualquer ≤ 5 MB) para o campo `arquivo`.

| # | Request | Demonstração |
|---|---------|--------------|
| 1 | `POST /api/submissoes/{submissaoId}/documentos` — multipart: `arquivo` (PDF) + `tipoDocumento=HISTORICO_ESCOLAR` | **201** — `statusValidacao=PENDENTE` (**documentoId=1**) |
| 2 | `GET /api/submissoes/{submissaoId}/documentos` | **200** — metadados (tipo, nome original, tamanho, mime, status — RF15 depois valida na coleção completa) |

*Fala:* anexos persistem em disco (`armazenamento`) com metadados no banco; só `application/pdf`, máx 5 MB (provas: `ARQUIVO_INVALIDO`, `ARQUIVO_MUITO_GRANDE` — coleção completa).

## Bloco 7 — Notificações (R · U) · 30 s

Pasta **Notificações**. Header **`X-Usuario-Id: 1`** (aluno).

| # | Request | Demonstração |
|---|---------|--------------|
| 1 | `GET /api/notificacoes` | **200** — inbox com a notificação do **envio** do bloco 5: "Submissão #2026-0001: RASCUNHO → RECEBIDA" |
| 2 | `PATCH /api/notificacoes/{notificacaoId}/lida` (id 1) | **200** + body — lida (**idempotente**: rode duas vezes → 200 nas duas) |

*Fala:* notificação por transição, engatilhada **na mesma transação** do envio (RF17) — a prova de transições do gerenciador (RF16, PATCH /status) está na coleção completa.

---

## Cobertura por bloco (referência rápida para a arguição)

| Critério | Prova neste roteiro |
|----------|---------------------|
| **C1** — Entidades ↔ DER | Bloco 2: usuário criado (4) → aluno com **o mesmo id** (`MapsId`); protocolo null → preenchido no envio (bloco 5) |
| **C2** — Repositories Spring Data com consultas de negócio | Blocos 1/3: filtro por função, paginação, `/abertos` por janela |
| **C3** — Regras nos Services (nada de `if` no controller) | Bloco 5: protocolo RN02, snapshot RN09, notificação RF17 na mesma transação |
| **C4** — Controllers HTTP coerentes | 201+`Location` nos POSTs · 200 nos GET/PUT/PATCH · D2 `{status, erro, mensagem}` nos erros (plano B) |
| **C5** — Evidência (CRUD via coleção) | Essa demo: as 23 requests essenciais (~8 min) |

## Plano B — contra-provas de erro (coleção completa `sgba-checkpoint3.insomnia.json`, se pedirem)

| Request (pasta) | Esperado |
|-----------------|----------|
| `POST /api/usuarios` @gmail.com (1) | 400 `EMAIL_INSTITUCIONAL_INVALIDO` (RN01) |
| `POST /api/usuarios` e-mail duplicado (1) | 409 `EMAIL_JA_CADASTRADO` |
| `POST /api/programas` janela invertida (3) | 400 `PERIODO_INVALIDO` |
| `POST /api/editais` nº/ano duplicado (4) | 409 `EDITAL_DUPLICADO` |
| `POST /api/submissoes` Edital C (5) e `enviar` | 409 `FORA_DO_PRAZO` (RN05) |
| `POST /enviar` sem respostas/termo (5) | 409 `ENVIO_INCOMPLETO` com pendências (RN12) |
| `PATCH /api/submissoes/{id}/status` gerenciador (5) | RF16: `EM_ANALISE` → `PENDENTE_DOCUMENTOS` (motivo) → `APROVADA`; sem motivo → 400 `MOTIVO_OBRIGATORIO` (RN04); aluno → 400 `PERMISSAO_INSUFICIENTE` |
| `POST` documento `.txt` (6) · `PATCH /{id}/status` (6) | 400 `ARQUIVO_INVALIDO` · 200 com `observacao` (RF15) |
| `POST /{id}/encerrar` ×2 (3) | 200 → 409 `PROGRAMA_JA_ENCERRADO` |

> **Testes automatizados como reforço (D8):** `./mvnw test` roda a suíte com H2 — usuários/alunos, editais, submissões (RN08/RN10/RN05/RN09/RN12/RF16/§9.2), documentos e notificações.
