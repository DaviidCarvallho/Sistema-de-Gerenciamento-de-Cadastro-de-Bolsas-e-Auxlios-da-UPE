# Convenções do projeto — Checkpoint 3 (Issue 1)

Documento de referência para os PRs das Issues 2–6. Fonte completa: `docs/superpowers/specs/2026-11-02-checkpoint3-backend-issues-design.md` (§4 e §9).

## Decisões técnicas D1–D9 (obrigatórias nos módulos)

| Decisão | Valor | Efeito prático nos módulos |
|---|---|---|
| **D1 — "Usuário corrente" sem auth** | Header `X-Usuario-Id: <id>`; o Service valida existência/função. | Endpoints "minhas?" (RN06) validam dono do recurso com o id do header. No cp4 troca por `@AuthenticationPrincipal` sem mudar assinaturas de Service. Sem Spring Security, `senhaHash` recebe placeholder no seed ("cp3-sem-auth-substituido-no-cp4"). |
| **D2 — Tratamento de erro mínimo** | `config/GlobalExceptionHandler` (`@RestControllerAdvice`) + `RegraNegocioException` (carrega status HTTP; **default 409**; 400 p/ regra de "argumento inválido" — RN01, RN04, janelas), `RecursoNaoEncontradoException` (404), `ErrorResponse` `{status, erro, mensagem}`. | Services lançam `RegraNegocioException("CODIGO_ERRO", "mensagem")` (409) ou `new RegraNegocioException(HttpStatus.BAD_REQUEST, "CODIGO", "msg")` (400), e `RecursoNaoEncontradoException.porId("Entidade", id)` (404). Nunca devolver 200/500 inconsistentes. Prova: `GET /api/inexistente` → 404 com `ErrorResponse`. |
| **D3 — DTOs** | Java `record`s com sufixos `Request`/`Response` no pacote `dto`, mapeamento manual por métodos estáticos `from(...)`/`toEntity(...)`. Sem MapStruct. | Nomes: `<Entidade>CreateRequest`/`<Entidade>UpsertRequest`, `<Entidade>Response`, respostas derivadas (`<Entidade>ComFormularioResponse`, `SubmissaoPainelResponse` etc.). |
| **D4 — Paginação** | Listagens de catálogo usam `Pageable` (`?page=0&size=10&sort=titulo,asc`). | Controllers de catálogo (Programas, Editais, painéis) retornam `Page<TResponse>`. |
| **D5 — Persistência** | `GenerationType.IDENTITY` + `ddl-auto=update` no dev (Flyway no cp futuro); H2 no profile `test`. | Não criar tabelas de sequência; PKs auto-incrementais. |
| **D6 — Protocolo (RN02)** | `#AAAA-XXXX` com contador anual (`count(*) where protocolo like 'AAAA-%'`), gerado em transição `enviar` com retry (máx. 5 tentativas ante `DataIntegrityViolationException` da UK). | Submissão nasce com protocolo null (RASCUNHO); preenchido apenas no envio. |
| **D7 — Armazenamento de arquivos (RNF06)** | Diretório local `sgba.storage.dir` (default `./storage`, configurável em `application.properties`); entidades gravam caminho relativo. | Uploads (Issues 4/6) salvam arquivo no diretório e persistem só metadados. |
| **D8 — Testes** | Testes automatizados opcionais (a evidência formal é a coleção Postman); os existentes (context + handler + `/api/inexistente`) precisam permanecer verdes. | `./mvnw test` usa H2; novos testes devem usar `@ActiveProfiles("test")` e `RestTestClient`. |
| **D9 — Branch/PR** | `feature/issue-N-<tema>`, 1 issue = 1 PR, PR pequeno (< ~600 linhas). | Issues 2–6 não alteram `GlobalExceptionHandler`/`DevSeedData` (donos: Issue 1); apenas consomem as exceções. |

## Padrão de erros (exemplos de códigos)

- 404: `RECURSO_NAO_ENCONTRADO` (rota inexistente ou `id` inexistente — não vazar existência em RN06: responder 404).
- 409: `SUBMISSAO_DUPLICADA`, `EDITAL_DUPLICADO`, `EMAIL_JA_CADASTRADO`, `ALUNO_JA_EXISTENTE`, `ORDEM_JA_USADA`, `TIPO_JA_EXIGIDO`, `FORA_DO_PRAZO`, `ENVIO_INCOMPLETO`, `STATUS_NAO_EDITAVEL`, `TRANSICAO_INVALIDA`, `PERIODO_JA_ENCERRADO`.
- 400: `EMAIL_INSTITUCIONAL_INVALIDO`, `PERIODO_INVALIDO`, `JANELA_INVALIDA`, `MOTIVO_OBRIGATORIO`, `PERMISSAO_INSUFICIENTE`, `ORIENTADOR_INVALIDO`, `ARGUMENTO_INVALIDO` (payload/parameter mal formado, genérico do handler).

## Regras de camadas (RNF02)

`controller` (só HTTP: verbo/status/_Location_, sem `if` de negócio) → `service` (RNs + transações) → `repository` (Spring Data, consultas de negócio) — DTOs (`dto`) atravessam as bordas; entidades JPA (`entity`) ficam nas camadas internas.
