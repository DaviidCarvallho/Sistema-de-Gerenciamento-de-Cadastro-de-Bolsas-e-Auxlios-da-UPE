# Auditoria final do Checkpoint 3 — SGBA Back-end

**Data:** 2026-11-06 · **Branch:** `feature/issues-5-6-final-checkpoint3` (Issues 5 + 6 fechadas)
**Fonte dos critérios:** PDF da disciplina (C1–C5) + `docs/superpowers/specs/2026-11-02-checkpoint3-backend-issues-design.md` (§1/§7) + `docs/decisoes-checkpoint3.md` (D1–D9).

| Critério (peso) | Como é atendido | Evidência concreta neste repo |
|---|---|---|
| **C1 — Entidades JPA correspondem ao DER validado no cp2** | Auditoria campo-a-campo das 11 entidades + 7 enums × `der.dbml` v3, com divergências corrigidas e justificadas (v2→v3) | `docs/auditoria-entidades-der.md` (PR da Issue 1); entidades atuais conforme auditoria: `Submissao` **sem FK** `programa_id` (programa via `edital.programa`), `protocolo` **nullable** (nasce null em RASCUNHO, preenchido no `enviar` — comprovável na coleção `5 · Submissões` A1/A6), 5 constraints de unicidade (`uk_aluno_edital`, `uk_resposta_unica`, `uk_edital_numero_ano`, `uk_pergunta_ordem`, `uk_exigencia_edital_tipo`) declaradas nas entidades |
| **C2 — Repository implementado para as entidades core** | **11/11 entidades core têm repository** Spring Data com consultas de negócio (não apenas CRUD básico) | `repository/`: Usuario (RN01/RN10, `exigeFuncao`), Aluno (cpf/matricula), ProgramaBolsa (janela por datas RF05), Edital (RF05/uk), PerguntaEdital (ordem), ExigenciaEdital (tipo), **Submissao** (RN08 `existsByAlunoIdAndEditalId`, `findByAlunoId`, `findByProtocolo`, contador RN02, filtro RF14 `@Query`), **RespostaSubmissao** (upsert `uk_resposta_unica`), **HistoricoStatus** (RF12 ordenado), **DocumentoAnexo** (RF12 `existsBySubmissaoIdAndTipoDocumento` p/ RN12, metadados), **Notificacao** (RF17 inbox/não-lidas) |
| **C3 — Service contém regra de negócio real** | **Todas as RNs do DRS implementadas nos Services** (controllers sem `if` de negócio — RNF02): | Submissões — **RN02** protocolo `#AAAA-XXXX` anual + retry (D6) `SubmissaoService.gerarProtocolo`; **RN03** edição só RASCUNHO (+ PENDENTE_DOCUMENTOS p/ documentos — Issue 6); **RN04** motivo p/ PENDENTE_DOCUMENTOS/INDEFERIDA; **RN05** janela de envio (Edital C → `FORA_DO_PRAZO`); **RN06** dono OU gerenciador, senão 404 sem vazar (404/400 por contexto); **RN07** histórico append-only na mesma transação; **RN08** unicidade (aluno,edital); **RN09** snapshot `curso/periodo/cra/unidadeMomento` (conferível: depois de mudarem CRA, `craMomento` permanece); **RN10** orientador PROFESSOR; **RN12** termo + perguntas obrigatórias + exigências obrigatórias com documento. Usuários — **RN01** `@upe.br`. Programas — janela com datas efetivas. Editais — `JANELA_INVALIDA`, uniques. Documentos — **RN11** `OUTRO` ↔ `descricaoPersonalizada`; RF15 validação com observação obrigatória |
| **C4 — Controller com endpoints REST coerentes** | **Verbos/status coerentes e dokumentados**: 201+`Location` (criações), 200 (get/put/patch/envio), 204 (deletes), 400/404/409 via `GlobalExceptionHandler` base (D2) com body `{status, erro, mensagem}` | Mapa completo no README §6.1; coleção Insomnia/Postman prova casa não-status-aleatório (ex.: `POST /{id}/enviar` = **200**, não 201; delete = **204**; parâmetro com tipo errado → 400; rota inexistente → 404) |
| **C5 — Evidência de funcionamento (CRUD via Postman/Insomnia)** | **Coleção Insomnia completa (85 requests, 8 pastas — Issues 1–6)** + coleção Postman equivalente + fragmentos por módulo preenchidos + seed idempotente + **roteiro de demo < 10 min** | `docs/insomnia/sgba-checkpoint3.insomnia.json` (`jq` válido); `docs/postman/sgba-checkpoint3.postman_collection.json`; `docs/demo-checkpoint3.md`; `config/DevSeedData` (contrato público de id) |

## Cobertura de RFs citados no checkpoint (escopo cp3)

| RF | Onde está | Evidência |
|---|---|---|
| RF01–RF02 (perfil/informações base) | Módulo Usuários/Alunos (Issue 2) | pasta 1/2 da coleção |
| RF03/RF05 (programas, catálogo, encerrar) | Módulo Programas (Issue 3) | pasta 3 |
| RF04/RF06/RF07.1 (editais, formulário dinâmico, janela) | Módulo Editais (Issue 4) | pasta 4 |
| RF07.2/RF07.3 (wizard rascunho+respostas, RN08/RN10) | `SubmissaoController` POST/PUT | pasta 5 (A1–A5) |
| RF12 (timeline) | `GET /{id}/historico` | pasta 5 (E3 — 4 eventos) |
| RF11/RF14 (painéis) | `/api/submissoes/minhas` + `GET /api/gerenciador/submissoes` | pasta 5 (E2, E5) |
| RF16 (transições de status **simplificadas sem auth**) | `PATCH /api/submissoes/{id}/status` (tabela RF16 + RN04 + §9.2) | pasta 5 (D1–D7) |
| RF09 (anexos) | `POST /{id}/documentos` + download | pasta 6 |
| RF15 (validação do anexo pelo gerenciador) | `PATCH /api/documentos/{id}/status` | pasta 6 (D8/D9) |
| RF17 (inbox) | `/api/notificacoes` + `/lida` | pasta 7 |

## Convenções exigidas pela disciplina (RNF02/D1–D9)

- **Camadas:** controller (só HTTP) → service (RNs + `@Transactional`) → repository (Spring Data) — verificável em `SubmissaoController`→`SubmissaoService` (sem `if` de negócio no controller).
- **DTOs:** Java `record` com sufixos `Request`/`Response` entre camadas; mapeamento manual estático `from(...)`.
- **Sem Spring Security (cp4):** usuário corrente via header `X-Usuario-Id` (D1) e helper `exigeFuncao` — mudança para cp4 não quebra assinaturas de Service.
- **Sem Flyway (cp4):** `ddl-auto=update` em dev contra o PG do `compose.yaml` (D5) e H2 no profile `test` (D8).
- **Arquivos em disco (D7):** `sgba.storage.dir` (edital 10 MB; documento 5 MB — propriedades no `application.properties`).

## O que fica de fora (explícito no escopo — cp4/cp5)

- Spring Security/RBAC/auth (cp4 — troca do header por credencial), Bean Validation completo + validação de conteúdo do arquivo além do content-type (cp4), Flyway, integração com front (cp5), geração de PDF/CSV (futuro), notificações por e-mail.

## Checklist de santidade executado nesta branch

- [x] `./mvnw test` **verde** (profiles test/H2): 8 classes de teste (contexto, handler, rota inexistente, usuário/aluno, edital, **notificações**, **submissões 20 testes**, **documentos 10 testes**).
- [x] Smoke ao vivo com PG (`docker compose up -d`) + app `dev`: banco limpo → seed → fluxo completo por HTTP (rascunho → respostas → enviar → PATCH EM_ANALISE/PENDENTE/APROVADA → histórico com 4 eventos → upload multipart → download com bytes iguais → inbox do aluno com as notificações das transições) — sem 500.
- [x] `GET /api/gerenciador/submissoes` **sem filtros** no PostgreSQL real (todos os parâmetros nulos do `@Query`) → 200 com 2 registros (recomendação do review final).
- [x] Coleções JSON válidas (`jq empty`): Insomnia (86 requests) e Postman (8 pastas).
- [x] Um único PR fecha as duas issues restantes (#5 e #6) — decisão do time nesta iteração final (D9 ajustado no PR).

## Limitações documentadas (cp4)

> Em `POST /api/submissoes/{id}/documentos` com **`tipoDocumento` ausente** no multipart, o Spring responde 400 com body fora do formato D2 (`MissingServletRequestPartException` não mapeado no handler base — Issue 1 não é dona do arquivo para este cp3). Que fica para Bean Validation/handler completo do cp4, junto com CORS/auth.

> Quatro consultas de repository previstas no plano **sem chamador hoje** (usadas por cp4/consumidor futuro): `SubmissaoRepository.findByProtocolo`, `findByEditalId`, `DocumentoAnexoRepository.findByIdAndSubmissaoId`, `NotificacaoRepository.countByDestinatarioIdAndLidaEmIsNull`.
