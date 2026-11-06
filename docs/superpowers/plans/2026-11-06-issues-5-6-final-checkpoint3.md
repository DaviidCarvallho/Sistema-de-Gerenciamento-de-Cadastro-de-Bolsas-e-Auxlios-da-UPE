# Issues 5+6 — Submissões, Documentos, Notificações e Evidência final do Checkpoint 3

> **Execução agente:** sub-skill obrigatória — rodar **superpowers:executing-plans** task-a-task (ou superpowers:subagent-driven-development). Passos usam checkbox (`- [ ]`). Cada task termina em commit. Respeitar **na ordem**: T0 → T1 → T2 → T3 → T4 → T5 → T6 → T7 → T8 → T9.

**Goal:** fechar o Checkpoint 3: Issue 5 (módulo de Submissões — ciclo de vida completo) + Issue 6 (Documentos + Notificações + evidência final), garantindo C1–C5 com coleção Insomnia completa para o professor.

**Architecture:** segue o padrão do repo `controller → service → repository` com DTOs records entre camadas; Services concentram as RNs; transições de status seguem tabela RF16 fixa; notificações entram na submissão por uma porta (`service/port/NotificacaoPort`) implementada por `NotificacaoService`; arquivos persistem em disco via `ArmazenamentoService` (D7).

**Tech Stack:** Java 21, Spring Boot 4.x, Spring Data JPA (PostgreSQL dev / H2 test), Lombok, sem novas dependências.

**Spec:** `docs/superpowers/specs/2026-11-02-checkpoint3-backend-issues-design.md` (§6.5, §6.6, §4, §9) + `docs/decisoes-checkpoint3.md` (D1–D9). **Branch única** para a iteração: `feature/issues-5-6-final-checkpoint3` (pedido da dupla; commits separados por task; 1 PR fechando #5 e #6).

## Global Constraints

Biblioteca de padrões do repo (copiar valores à risca nos testes e bodies):

- **D1** — usuário corrente por header `X-Usuario-Id` (controllers declaram `@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId`; header ausente/nulo → 400 `ARGUMENTO_INVALIDO` "Usuário não informado (header X-Usuario-Id)" — padrão de `UsuarioService.exigeFuncao`). Nenhum handler novo.
- **D2** — erros sempre `{ "status": int, "erro": codigo, "mensagem": string }` (`GlobalExceptionHandler`). Quedas de enum inválido no body (Jackson `HttpMessageNotReadableException`) retornam 400 do handler — não inventar parâmetro de enum “tolerante”.
- **D3** — DTOs: `record` no pacote `dto`, mapeamento estático `from(...)`. Sufixos: `<X>Request`, `<X>Response`.
- **D6** — protocolo na transição `enviar`: `#AAAA-XXXX` (ex. `#2026-0001` — 10 caracteres, coluna `protocolo length=10`); AAAA = ano corrente do envio; contador anual = `existsByProtocolo` com incremento local a partir de `count(ano)`; máx. 5 tentativas; singel final → 409 `PROTOCOLO_INDISPONIVEL`.
- **D7** — arquivos em `sgba.storage.dir` (default `./storage`); **documento de submissão**: só `application/pdf`, máx. **5 MB**, propriedade nova `sgba.storage.documento-max-bytes=5242880`.
- **D8** — `./mvnw test` (profile `test`, H2) verde em toda task; novos testes com `@ActiveProfiles("test")`, `RestTestClient`, helpers via `EntityManager`/`TransactionTemplate` (padrão de `EditalApiTest`).
- **D9** — arquivos-collection rules: **não** alterar `GlobalExceptionHandler` nem `DevSeedData`; `ArmazenamentoService` (criado pela Issue 4) pode ser **estendido aditivamente**.
- **Seed (banco limpo, IDs do log):** usuarios 1=aluno@upe.br (ALUNO, perfil completo), 2=professor@upe.br, 3=gerenciador@upe.br; programa 1; editais A(id=1, janela aberta, 1 pergunta obrigatória + 2 exigências opcionais), B(id=2, janela aberta, exigências **obrigatórias**), C(id=3, janela **encerrada** → RN05), D(id=4, sem formulário).
- **Tabela RF16 (única fonte de transições via `PATCH /status`):**
  - `RECEBIDA → [EM_ANALISE]`
  - `EM_ANALISE → [APROVADA, INDEFERIDA, PENDENTE_DOCUMENTOS]`
  - `PENDENTE_DOCUMENTOS → [EM_ANALISE, APROVADA, INDEFERIDA]`
  - `RASCUNHO`, `APROVADA`, `INDEFERIDA` → sem transição via PATCH (409 `TRANSICAO_INVALIDA`). `RASCUNHO → RECEBIDA` sai **só** por `POST /{id}/enviar`.
- **Códigos novos desta iteração:** 409 `SUBMISSAO_DUPLICADA`, `FORA_DO_PRAZO`, `STATUS_NAO_EDITAVEL`, `ENVIO_INCOMPLETO`, `TRANSICAO_INVALIDA`, `PROTOCOLO_INDISPONIVEL`; 400 `ORIENTADOR_INVALIDO`, `ALUNO_SEM_PERFIL`, `MOTIVO_OBRIGATORIO`, `PERMISSAO_INSUFICIENTE`, `DESCRICAO_OBRIGATORIA` (RN11), `OBSERVACAO_OBRIGATORIA` (RF15), `ARQUIVO_INVALIDO`, `ARQUIVO_MUITO_GRANDE`, `ARGUMENTO_INVALIDO`; 404 `RECURSO_NAO_ENCONTRADO`.
- **Decisões documentadas (minhas chamadas, registrar no PR):**
  1. **RN06** consulta (detalhe/histórico/listar docs/download): dono **ou** `GERENCIADOR` → 200; caso contrário **404 sem vazar existência** (fail-silent, regra da §6.5 — differente do 400 de `exigeFuncao`, que fica só para endpoints de **escrita** do gerenciador).
  2. **Escrita do aluno** (rascunho, enviar, upload/delete de documento): **dono apenas** (outra pessoa, inclusive gerenciador → 404).
  3. **RF15** validação do documento: quando `statusValidacao ∈ {INCOMPLETO, VENCIDO}`, `observacaoValidacao` é obrigatória → 400 `OBSERVACAO_OBRIGATORIA` (análogo a RN04).
  4. **RN09** sem perfil de aluno → 400 `ALUNO_SEM_PERFIL` "Complete o perfil de aluno antes de enviar" (evita 500/NPE; seed tem perfil).
  5. `DocumentoUploadResponse` unificado em **`DocumentoResponse`** (mesma forma na resposta 201 e na listagem — DRY); multipart usa campo **`arquivo`** (não `file`, RF09 do §6.6).
  6. `RF14` (`GET /api/gerenciador/submissoes`) fica **aberto** como demais endpoints de consulta (§9.2 delimita checagem de papel a 2 endpoints); formar para cp4.
  7. `submissaoId` no fluxo — Insomnia: env var e instrução para preencher após a criação (IDs sequenciais por tabela em banco limpo: 1ª submissão id=1).

## Review Focus

Entradas que o spec implicitamente cobre e que mais provam bug na demo — cada linha já tem teste criador encaixado na task correspondente:

1. **Header `X-Usuario-Id` ausente** em POST rascunho/PATCH status → 400 (não 500): conectado por teste em T4.
2. **Consulta por não-dono sem função** (RN06) → **404** em detalhe/histórico/docs: T6.
3. **PATCH status sem motivo** para `INDEFERIDA`/`PENDENTE_DOCUMENTOS` → 400 `MOTIVO_OBRIGATORIO` (RN04): T6.
4. **Doc não-PDF / >5MB** no upload de documento → 400: T7.
5. **Protocolos anuais únicos** — dois envios no mesmo ano → `#2026-0001` e `#2026-0002` (sem colisão): T5.
6. **Resposta/pergunta de outro edital** no PUT → 404 (não cruza dados entre editais): T4.

---

### Task 0: Branch e baseline

**Files:** nenhum (git).

- [ ] **Step 1: criar branch a partir de `main` atualizado**

```bash
git checkout main && git pull origin main
git checkout -b feature/issues-5-6-final-checkpoint3
```

- [ ] **Step 2: baseline de testes verde**

Run: `./mvnw test`
Expected: BUILD SUCCESS (Testes existentes: contexto, handler, usuário/aluno, edital).

- [ ] **Step 3: commit de baseline**

```bash
git commit --allow-empty -m "chore: baseline da iteração final (Issues 5+6) - checkpoint 3"
```

### Task 1: Repositories das 5 entidades do fluxo (C2)

**Files:**
- Create: `src/main/java/br/edu/upe/sgba/repository/SubmissaoRepository.java`
- Create: `src/main/java/br/edu/upe/sgba/repository/RespostaSubmissaoRepository.java`
- Create: `src/main/java/br/edu/upe/sgba/repository/HistoricoStatusRepository.java`
- Create: `src/main/java/br/edu/upe/sgba/repository/DocumentoAnexoRepository.java`
- Create: `src/main/java/br/edu/upe/sgba/repository/NotificacaoRepository.java`

**Interfaces (produz):**
- `SubmissaoRepository extends JpaRepository<Submissao, Long>`:
  `List<Submissao> findByAlunoIdOrderByCriadoEmDesc(Long alunoId)`; `boolean existsByAlunoIdAndEditalId(Long alunoId, Long editalId)`; `Optional<Submissao> findByProtocolo(String protocolo)`; `boolean existsByProtocolo(String protocolo)`; `Page<Submissao> findByEditalId(Long editalId, Pageable pageable)`.
  + consulta RF14 (filtros combinados):
  ```java
  @Query("""
          select s from Submissao s
          where (:status is null or s.status = :status)
            and (:editalId is null or s.edital.id = :editalId)
            and (:programaId is null or s.edital.programa.id = :programaId)
          """)
  Page<Submissao> filtrar(@Param("status") StatusSubmissao status,
                          @Param("editalId") Long editalId,
                          @Param("programaId") Long programaId,
                          Pageable pageable);
  ```
- `RespostaSubmissaoRepository`: `List<RespostaSubmissao> findBySubmissaoId(Long submissaoId)`; `Optional<RespostaSubmissao> findBySubmissaoIdAndPerguntaId(Long submissaoId, Long perguntaId)`.
- `HistoricoStatusRepository`: `List<HistoricoStatus> findBySubmissaoIdOrderByDataHoraDesc(Long submissaoId)`.
- `DocumentoAnexoRepository`: `List<DocumentoAnexo> findBySubmissaoId(Long submissaoId)`; `Optional<DocumentoAnexo> findByIdAndSubmissaoId(Long id, Long submissaoId)`; `boolean existsBySubmissaoIdAndTipoDocumento(Long submissaoId, TipoDocumento tipoDocumento)`; `int countBySubmissaoId(Long submissaoId)`.
- `NotificacaoRepository`: `List<Notificacao> findByDestinatarioIdOrderByCriadoEmDesc(Long destinatarioId)`; `Optional<Notificacao> findByIdAndDestinatarioId(Long id, Long destinatarioId)`; `int countByDestinatarioIdAndLidaEmIsNull(Long destinatarioId)`.

- [ ] **Step 1: criar os 5 arquivos com os métodos acima** (Javadoc de 1 linha por regra que o método serve: RN08/RF14/RN12/RF17).
- [ ] **Step 2: compilação**

Run: `./mvnw compile -q`
Expected: BUILD SUCCESS.

- [ ] **Step 3: commit**

```bash
git add src/main/java/br/edu/upe/sgba/repository && git commit -m "feat(submissoes): repositories do fluxo de submissão (filtro RF14, RN08, RF17)"
```

### Task 2: Notificações — porta (`service/port`) + service + controller (RF17)

**Files:**
- Create: `src/main/java/br/edu/upe/sgba/service/port/NotificacaoPort.java`
- Create: `src/main/java/br/edu/upe/sgba/service/NotificacaoService.java`
- Create: `src/main/java/br/edu/upe/sgba/dto/NotificacaoResponse.java`
- Create: `src/main/java/br/edu/upe/sgba/controller/NotificacaoController.java`
- Test: `src/test/java/br/edu/upe/sgba/controller/NotificacaoApiTest.java`

**Interfaces:**
- `NotificacaoPort` (hook consumido em T5/T6):
  ```java
  void notificarTransicao(Long destinatarioId, Long submissaoId, String mensagem);
  ```
- `NotificacaoService implements NotificacaoPort` — injeta `NotificacaoRepository`, `UsuarioRepository` e `SubmissaoRepository` (usar `getReferenceById` para `destinatario`/`submissao`); `criadoEm = LocalDateTime.now()` ao notificar; `lidaEm` null.
  Guard de header (regra global): se `destinatarioId == null` → throw `new RegraNegocioException(HttpStatus.BAD_REQUEST, "ARGUMENTO_INVALIDO", "Usuário não informado (header X-Usuario-Id)")` — o Service é quem valida, **não o controller** (`Validacao` é package-private em `service`; `RegraNegocioException` é importável).
  `@Transactional(readOnly=true) List<NotificacaoResponse> listar(Long destinatarioId, boolean somenteNaoLidas)`
  `@Transactional NotificacaoResponse marcarLida(Long id, Long destinatarioId)` — `findByIdAndDestinatarioId` → senão 404 (`RecursoNaoEncontradoException.porId("Notificacao", id)`); **idempotente** (já lida → devolve sem erro).
- `dto/NotificacaoResponse(Long id, String mensagem, LocalDateTime criadoEm, boolean lida, Long submissaoId)` + `from(Notificacao)`.

**Contrato REST (Issue 6):**
| Verbo | Rota | Status |
|---|---|---|
| GET | `/api/notificacoes?somenteNaoLidas=true` | 200 (header `X-Usuario-Id`; lista `[]` nunca null) |
| PATCH | `/api/notificacoes/{id}/lida` | **200 + body atualizado** (padrão da issue; idempotente) / 404 |

- [ ] **Step 1: testes (`NotificacaoApiTest`)** — criar usuário via POST `/api/usuarios` (helper do teste) e usar esse id no header:
  1. `listar_inbox_vazio_retorna200` — GET → 200 body `[]`.
  2. `marcarLida_idInexistente_retorna404` — PATCH `/api/notificacoes/999999/lida` → 404 `$.erro == "RECURSO_NAO_ENCONTRADO"`.
  (notificarTransicao é coberto indiretamente em T5/T6; sem teste dedicado aqui.)
- [ ] **Step 2: rodar o teste e ver falhar** — Run: `./mvnw test -Dtest=NotificacaoApiTest -q` → Expected: FAIL (404 vs 200 na 1ª asserção; handler de rota inexistente).
- [ ] **Step 3: implementar port + service + controller + DTO** (controller sem `if` e sem lógica de header: só repassa o valor cru; `usuarioId == null` → 400 tratado no Service).
- [ ] **Step 4: rodar o teste e ver passar** — Run: `./mvnw test -Dtest=NotificacaoApiTest -q` → Expected: PASS.
- [ ] **Step 5: commit**

```bash
git add -A src/main/java/br/edu/upe/sgba/{service,dto,controller} src/test && git commit -m "feat(notificacoes): porta NotificacaoPort + inbox RF17 com marcar-lida idempotente"
```

### Task 3: DTOs de Submissão ( Issues 5) — apenas a base do rascunho/painel/detalhe

**Files:**
- Create: `dto/SubmissaoRascunhoRequest.java`, `dto/RespostaUpsertRequest.java`, `dto/SubmissaoUpdateRequest.java`, `dto/SubmissaoDetalheResponse.java`, `dto/SubmissaoPainelResponse.java`
- (T5 adiciona): `dto/SubmissaoEnviadaResponse.java`, `dto/HistoricoResponse.java` · (T6 adiciona): `dto/DecisaoRequest.java`

**Interfaces (registros exatos):**
```java
record SubmissaoRascunhoRequest(Long editalId, String tituloProjeto, String projetoResumo,
                                String planoTrabalho, Long orientadorId) {}

record RespostaUpsertRequest(Long perguntaId, String respostaTexto) {}

record SubmissaoUpdateRequest(String tituloProjeto, String projetoResumo, String planoTrabalho,
                              Long orientadorId, List<RespostaUpsertRequest> respostas,
                              Boolean termoAceite) {}   // editalId não muda no PUT (ignorado)

record SubmissaoDetalheResponse(Long id, String protocolo, StatusSubmissao status,
        String tituloProjeto, String projetoResumo, String planoTrabalho, Boolean termoAceite,
        LocalDateTime dataEnvio, LocalDateTime criadoEm,
        EditalResumoResposta edital, OrientadorResumoResposta orientador,
        List<RespostaView> respostas, Integer qtdDocumentos,
        String cursoMomento, Short periodoMomento, BigDecimal craMomento, String unidadeMomento) {
    record EditalResumoResposta(Long id, String numeroEdital, Short ano,
            LocalDateTime dataInicioSubmissoes, LocalDateTime dataFimSubmissoes, String programaTitulo) {}
    record OrientadorResumoResposta(Long id, String nome) {}
    record RespostaView(Long perguntaId, String enunciado, Boolean obrigatoria, String respostaTexto) {}
    static SubmissaoDetalheResponse from(Submissao s, List<RespostaSubmissao> respostas, int qtdDocs) {...}
}

record SubmissaoPainelResponse(Long id, String protocolo, StatusSubmissao status, String tituloProjeto,
        LocalDateTime dataEnvio, Long editalId, String editalTitulo, String programaTitulo,
        Integer qtdDocumentos) {
    static SubmissaoPainelResponse from(Submissao s, int qtdDocs) {...} // editalTitulo = numero + "/" + ano
}
```

- [ ] **Step 1: criar os 5 records com mapeamento `from(...)` estático.**
- [ ] **Step 2: compile** — Run: `./mvnw compile -q` → BUILD SUCCESS.
- [ ] **Step 3: commit**

```bash
git add src/main/java/br/edu/upe/sgba/dto && git commit -m "feat(submissoes): DTOs de rascunho/PUT/detalhe/painel (D3)"
```

### Task 4: SubmissaoService — rascunho + PUT + minhas + detalhe (RN08/RN10/RN03/RN06)

**Files:**
- Create: `service/SubmissaoService.java` (métodos do rascunho; T5/T6 adicionam os demais)
- Create: `controller/SubmissaoController.java`
- Test: `test/controller/SubmissaoApiTest.java`

**Interfaces (produz/consumido):**
- `SubmissaoService` (injeta: `SubmissaoRepository submissoes`, `RespostaSubmissaoRepository repositoryRespostas`, `DocumentoAnexoRepository repositoryDocumentos`, `EditalRepository repositoryEditais`, `PerguntaEditalRepository repositoryPerguntas`, `UsuarioService usuarios`, `AlunoRepository repositoryAlunos`, `NotificacaoPort notificacoes` — criada em T2):
  ```java
  @Transactional SubmissaoDetalheResponse criar(Long alunoId, SubmissaoRascunhoRequest req)   // RN08/RN10
  @Transactional(readOnly=true) List<SubmissaoPainelResponse> minhas(Long usuarioId)           // RF11
  @Transactional(readOnly=true) SubmissaoDetalheResponse detalhe(Long id, Long usuarioId)      // RN06
  @Transactional SubmissaoDetalheResponse atualizar(Long id, Long usuarioId, SubmissaoUpdateRequest req) // RN03
  // helpers privados:
  // void exigirUsuarioId(Long usuarioId)  → usuarioId == null → 400 ARGUMENTO_INVALIDO (chamado no INÍCIO de TODOS os métodos, inclusive consultas)
  // Submissao buscar(Long id)             → 404 RecursoNaoEncontrado
  // void assegurarVisibilidade(Submissao s, Long usuarioId) → dono OK | usuario existe && GERENCIADOR OK | senão 404 (RN06, sem vazar existência)
  ```
- Regras no service (nada de `if` no controller):
  - **RN08** criar: `editalId` obrigatório → edital 404; `existsByAlunoIdAndEditalId(usuarioId, editalId)` → 409 `SUBMISSAO_DUPLICADA`; título/teste campos via `Validacao.obrigatorio`; `orientadorId` != null → usuario `funcao == PROFESSOR` senão **400** `ORIENTADOR_INVALIDO`; nasce em `status=RASCUNHO`, `termoAceite=false`, protocolo/dataEnvio/snapshot null → 201 pelo controller com `Location=/api/submissoes/{id}`.
  - **RN03-PUT**: só `status==RASCUNHO` → senão 409 `STATUS_NAO_EDITAVEL`; aplicar só campos não-nulos; `respostas[]` = **upsert** por `perguntaId` (se respostas null → não tocar em respostas): validar pergunta existente em `repositoryPerguntas` e **pertence ao edital da submissão** (сенão 404 "Pergunta");
  - **RN06 detalhe/histórico/documentos**: dono → ok; senão usuario existe && `funcao==GERENCIADOR` → ok; senão `RecursoNaoEncontrado` 404 (sem leak).
- Controllers:
  ```java
  POST   /api/submissoes                        → 201 + Location (header aluno)
  GET    /api/submissoes/minhas                 → 200 List<SubmissaoPainelResponse> (header)
  GET    /api/submissoes/{id}                   → 200 SubmissaoDetalheResponse (RN06)
  PUT    /api/submissoes/{id}                   → 200 SubmissaoDetalheResponse (dono, RN03)
  ```

- [ ] **Step 1: testes (`SubmissaoApiTest`) — helpers por EntityManager:** criar Programa (helper de `EditalApiTest`), Edital com janela aberta (agora−1d/now+30d), Pergunta obrigatória de ordem 1; criar Usuario ALUNO via API + perfil Aluno via API.
  1. `criarRascunho_valido_retorna201ComLocation` → status 201, header `Location`, `$.status == "RASCUNHO"`, `$.protocolo` null/empty.
  2. `criarRascunho_duplicado_retorna409` → 2º POST mesmo edital → `$.erro == "SUBMISSAO_DUPLICADA"`.
  3. `criarRascunho_orientadorInvalido_retorna400` → orientadorId do aluno → `$.erro == "ORIENTADOR_INVALIDO"`.
  4. `criarRascunho_semHeader_retorna400` → sem `X-Usuario-Id` → 400 `ARGUMENTO_INVALIDO`.
  5. `atualizarRascunho_upsertRespostas_retorna200` → PUT com resposta da pergunta + termoAceite → 200, `$.respostas[0].respostaTexto` presente, `$.termoAceite == true`; 2º PUT mesma pergunta → **id único de resposta** (contagem inalizada pela resposta do detalhe length == 1). Assert key: `$.respostas.size() == 1` após reenvio.
  6. `atualizarRascunho_respostaDeOutroEdital_retorna404` → 404.
- [ ] **Step 2: rodar e ver falhar** — `./mvnw test -Dtest=SubmissaoApiTest -q` → FAIL/404-current.
- [ ] **Step 3: implementar service + controller.**
- [ ] **Step 4: rodar e ver passar** — Expected: PASS.
- [ ] **Step 5: commit**

```bash
git add -A src && git commit -m "feat(submissoes): rascunho/PUT/minhas/detalhe com RN08, RN10, RN03 e RN06"
```

### Task 5: `enviar` (RN02 + RN05 + RN09 + RN12 + RN07 + hook) e histórico (RF12)

**Files:**
- Modify: `service/SubmissaoService.java` (adicionar `enviar`, `historico` e helper `gerarProtocolo`)
- Modify: `dto/` — adicionar `SubmissaoEnviadaResponse`, `HistoricoResponse`
- Modify: `controller/SubmissaoController.java` (adicionar as 2 rotas)
- Test: estender `SubmissaoApiTest`

**Interfaces:**
```java
@Transactional SubmissaoEnviadaResponse enviar(Long id, Long usuarioId)          // dono apenas
@Transactional(readOnly=true) List<HistoricoResponse> historico(Long id, Long usuarioId)  // RN06
// dto/T5:
record SubmissaoEnviadaResponse(Long id, String protocolo, StatusSubmissao status, LocalDateTime dataEnvio) {}
record HistoricoResponse(Long id, StatusSubmissao statusAnterior, StatusSubmissao statusNovo,
                         String observacao, LocalDateTime dataHora, String usuarioNome) { static from(HistoricoStatus) {...} }
```
- Algoritmo do `enviar` (ordem obrigatória das checagens — determinística para os códigos de erro):
  1. `buscar(id)` → 404; **dono apenas** → senão 404 (decisão 2 dos Global Constraints).
  2. `status != RASCUNHO` → **409** `TRANSICAO_INVALIDA` ("A submissão não está em rascunho").
  3. **RN05**: `now` fora de `[dataInicioSubmissoes, dataFimSubmissoes]` → **409** `FORA_DO_PRAZO` (mensagem citando a data fim).
  4. **RN12**: coletar pendências (lista de strings; quando vazia prossegue):
     `termoAceite != true` → "Termo de aceite não aceite";
     para cada `PerguntaEdital obrigatoria` do edital sem resposta com `respostaTexto` não-branca → "Pergunta obrigatória não respondida: '<enunciado>'";
     para cada `ExigenciaEdital obrigatoria` do edital sem `DocumentoAnexo` do tipo → "Documento obrigatório não anexado: <TIPO_DOCUMENTO>";
     existindo ≥1 → **409** `ENVIO_INCOMPLETO` (mensagem com itens separados por " • ").
  5. **RN09**: `Aluno perfil = repositoryAlunos.findById(alunoId).orElse(null)`; null → **400** `ALUNO_SEM_PERFIL`; copiar `curso→cursoMomento`, `periodoAtual→periodoMomento`, `craAtual→craMomento`, `unidadeAcademica→unidadeMomento`.
  6. **RN02**: `protocolo = gerarProtocolo()` — `#`+ano corrente+`-`+`%04d` contador, partindo de `count(ano)`+1 com loop de 5 tentativas via `existsByProtocolo`; esgotar → 409 `PROTOCOLO_INDISPONIVEL`.
  7. `dataEnvio = now`; `status = RECEBIDA`.
  8. **RN07**: submit histórico `RASCUNHO→RECEBIDA`, `observacao="Envio realizado pelo aluno"`, `usuario=dono` (mesma transação).
  9. **hook**: `notificacoes.notificarTransicao(donoId, submissaoId, "Submissão " + protocolo + ": RASCUNHO → RECEBIDA")`.
- Controller:
  ```java
  POST  /api/submissoes/{id}/enviar     → 200 SubmissaoEnviadaResponse (não 201 — o recurso já existe)
  GET   /api/submissoes/{id}/historico  → 200 List<HistoricoResponse> (desc por dataHora, RN06)
  ```

- [ ] **Step 1: testes (extender `SubmissaoApiTest`)** — helpers do T4 + Edital **encerrado** (agora −10 d/−1 d):
  7. `enviar_semRespostaObrigatoria_retorna409ComPendencias` → PUT respostas vazias + termoAceite true → POST enviar → 409 `$.erro == "ENVIO_INCOMPLETO"`, `$.mensagem` contém enunciado da pergunta.
  8. `enviar_semTermoAceite_retorna409` → resposta ok, `termoAceite=false` → 409 `ENVIO_INCOMPLETO` contém "Termo de aceite".
  9. `enviar_foraDoPrazo_retorna409` → rascunho válido no edital encerrado → 409 `$.erro == "FORA_DO_PRAZO"`.
  10. `enviar_valido_geraProtocoloSnapshotEHistorico` → fluxo completo → 200 com `$.protocolo` regex `#\d{4}-\d{4}`, `$.status == "RECEBIDA"`; GET detalhe → snapshot == perfil atual do aluno; GET historico → size ≥ 1, `statusAnterior == "RASCUNHO"`.
  11. `enviar_protocolosUnicosNoMesmoAno` → segunda submissão (novo edital) mesmo fluxo → 200 e `protocolo` diferente do primeiro (e casa regex `#\d{4}-\d{4}` — **não** cifrar sufixo, o contador depende da ordem dos testes).
  12. `enviar_porNaoDono_retorna404` → header de outro usuário → 404 (RN06).
  13. `enviar_semPerfil_retorna400` → aluno **sem perfil** → 400 `ALUNO_SEM_PERFIL`.
- [ ] **Step 2: rodar e ver falhar** — Expected: FAIL nos tests 7–13.
- [ ] **Step 3: implementar** os passos do algoritmo (mensagens exatas acima).
- [ ] **Step 4: rodar e ver passar** — incluir `./mvnw test -q` (suite inteira) para garantir que `NotificacaoApiTest` e demais continuam verdes.
- [ ] **Step 5: commit**

```bash
git add -A src && git commit -m "feat(submissoes): envio com RN02/RN05/RN09/RN12 e histórico RF12 (hook de notificação)"
```

### Task 6: RF16 — PATCH status, RN04 (motivo), §9.2 (GERENCIADOR) + painel gerenciador RF14

**Files:**
- Modify: `service/SubmissaoService.java` (adicionar `mudarStatus` + `listarGerenciador`)
- Create: `dto/DecisaoRequest.java`, `controller/GerenciadorController.java`
- Modify: `controller/SubmissaoController.java`
- Test: estender `SubmissaoApiTest`

**Interfaces:**
```java
record DecisaoRequest(StatusSubmissao novoStatus, String motivo) {}
@Transactional SubmissaoDetalheResponse mudarStatus(Long id, Long gerenciadorId, DecisaoRequest req)
@Transactional(readOnly=true) Page<SubmissaoPainelResponse> listarGerenciador(StatusSubmissao status, Long editalId, Long programaId, Pageable pageable)
```
- Algoritmo do `mudarStatus` (ordem):
  1. `buscar(id)` → 404.
  2. `usuarios.exigeFuncao(gerenciadorId, GERENCIADOR)` → **400** `PERMISSAO_INSUFICIENTE` senão.
  3. `req.novoStatus() == null` → **400** `ARGUMENTO_INVALIDO`.
  4. Tabela RF16 (Global Constraints): transição não permitida (incl. mesmo status) → **409** `TRANSICAO_INVALIDA` ("Transição 'A' → 'B' não é permitida (RF16)").
  5. **RN04**: novo ∈ {`PENDENTE_DOCUMENTOS`, `INDEFERida`} e motivo branco → **400** `MOTIVO_OBRIGATORIO`.
  6. **RN07**: insert histórico (antigo→novo, `observacao=motivo`, `usuario=gerenciador`).
  7. atualiza status; **hook** notificação ao aluno com protocolo + transição (+ motivo).
- Controllers:
  ```java
  PATCH /api/submissoes/{id}/status                    → 200 SubmissaoDetalheResponse (header gerenciador)
  GET   /api/gerenciador/submissoes                    → 200 Page<SubmissaoPainelResponse>
        ?status=&editalId=&programaId=&page&size&sort
  ```

- [ ] **Step 1: testes** — continuar do fluxo do T5 (submissão enviada = `RECEBIDA`):
  14. `patchStatus_transicaoValida_emAnalise→pendente_retorna200EHistorico4Eventos` → PATCH `EM_ANALISE` (200) → PATCH `PENDENTE_DOCUMENTOS` com motivo (200) → PATCH `APROVADA` (200, motivo opcional) → GET historico → 4 eventos, `statusAnterior/statusNovo` coerentes e 1 com `observacao == motivo`; **hook conectado**: GET `/api/notificacoes` com header do aluno → ≥1 notificação contendo o protocolo e o motivo; PATCH `/api/notificacoes/{id}/lida` → 200, e **repitido** → 200 (idempotência provada no fluxo real).
  15. `patchStatus_transicaoInvalida_rascunhoParaAprovada_retorna409` → submissão nova RASCUNHO → PATCH `APROVADA` → 409 `$.erro == "TRANSICAO_INVALIDA"`.
  16. `patchStatus_motivoAusente_paraPendente_retorna400` → motivo null → 400 `$.erro == "MOTIVO_OBRIGATORIO"`.
  17. `patchStatus_comHeaderAluno_retorna400` → header do aluno → 400 `PERMISSAO_INSUFICIENTE` (§9.2).
  18. `patchStatus_semHeader_retorna400` → 400 `ARGUMENTO_INVALIDO`.
  19. `gerenciadorPainel_filtroStatus_retorna200EPaginado` → GET `?status=RECEBIDA` → 200, `totalElements >= 1`, itens possuem `programaTitulo`.
- [ ] **Step 2: rodar e ver falhar** — Expected: FAIL.
- [ ] **Step 3: implementar** service + `GerenciadorController` + rota PATCH.
- [ ] **Step 4: rodar e ver passar** + suite completa.
- [ ] **Step 5: commit**

```bash
git add -A src && git commit -m "feat(submissoes): RF16 transições de status, RN04 motivo, §9.2 gerenciador e painel RF14"
```

### Task 7: Documentos (RF09/RF15/RN03/RN11 + D7) e validação do gerenciador

**Files:**
- Modify: `service/ArmazenamentoService.java` (aditivo)
- Create: `service/DocumentoAnexoService.java`
- Create: `dto/DocumentoResponse.java`, `dto/DocumentoValidacaoRequest.java`, `dto/DocumentoDownload.java`
- Create: `controller/DocumentoAnexoController.java`
- Modify: `src/main/resources/application.properties` (`sgba.storage.documento-max-bytes=5242880`)
- Test: `test/controller/DocumentoApiTest.java`

**Interfaces:**
- `dto/DocumentoDownload(byte[] conteudo, String nomeOriginal)`.
- `dto/DocumentoResponse(Long id, TipoDocumento tipoDocumento, String descricaoPersonalizada, String nomeOriginal, Long tamanhoBytes, String mimeType, StatusValidacaoDocumento statusValidacao, String observacaoValidacao, Long submissaoId)` + `from(...)`.
- `dto/DocumentoValidacaoRequest(StatusValidacaoDocumento statusValidacao, String observacaoValidacao)`.
- `ArmazenamentoService` (aditivo — constructor recebe `@Value("${sgba.storage.documento-max-bytes:5242880}") long maxDocumentoBytes`):
  ```java
  /** PDF do documento da submissão: só application/pdf, máx. 5 MB (RF09/D7); caminho rel. submissoes/{id}/documento-{ts}-{sanitizado}.pdf */
  public String salvarDocumento(Long submissaoId, MultipartFile arquivo)
  /** Remove arquivo do armazenamento (uso no DELETE de documento). */
  public void excluir(String caminhoRelativo)
  ```
- `DocumentoAnexoService`:
  ```java
  @Transactional DocumentoResponse anexar(Long submissaoId, Long usuarioId, MultipartFile arquivo,
                                          TipoDocumento tipoDocumento, String descricaoPersonalizada)
  @Transactional(readOnly=true) List<DocumentoResponse> listar(Long submissaoId, Long usuarioId)          // RN06
  @Transactional(readOnly=true) DocumentoDownload baixar(Long documentoId, Long usuarioId)                 // RN06
  @Transactional void excluir(Long documentoId, Long usuarioId)                                            // RN03+dono; 204 no controller
  @Transactional DocumentoResponse validarStatus(Long documentoId, Long gerenciadorId, DocumentoValidacaoRequest req) // RF15
  ```
  - `anexar`: submissão 404 → **dono apenas** → **RN03** status ∈ {RASCUNHO, PENDENTE_DOCUMENTOS} senão 409 `STATUS_NAO_EDITAVEL` → **RN11** `OUTRO` com descrição branca → **400** `DESCRICAO_OBRIGATORIA` "Informe 'descricaoPersonalizada' (Tipo OUTRO)" → salvar via `salvarDocumento` → persistir `DocumentoAnexo(statusValidacao=PENDENTE)`.
  - `validarStatus`: 404 → `exigeFuncao(GERENCIADOR)` 400 → `req.statusValidacao() == null` → 400 `ARGUMENTO_INVALIDO` → se `statusValidacao ∈ {INCOMPLETO, VENCIDO}` e observação branca → 400 `OBSERVACAO_OBRIGATORIA` → aplica e retorna DocumentoResponse.
- `DocumentoAnexoController` (class-level `@RequestMapping("/api/documentos")`; **rotas anvladas da submissão ficam no `SubmissaoController`** — associação `controller/EditalUpload/...` pattern):
  ```java
  // em SubmissaoController (multipart):
  POST /api/submissoes/{id}/documentos    → 201 DocumentoResponse + Location /api/documentos/{id}
                                             (@RequestPart("arquivo") MultipartFile arquivo,
                                              @RequestPart("tipoDocumento") String tipoDocumento,
                                              @RequestPart(value="descricaoPersonalizada", required=false) String descricaoPersonalizada)
                                              // conversão do tipoDocumento no service (enum inválido → 400)
  GET  /api/submissoes/{id}/documentos    → 200 List<DocumentoResponse> (RN06)
  // em DocumentoAnexoController:
  GET   /api/documentos/{id}/download     → 200 application/pdf + Content-Disposition attachment; nome original
  DELETE /api/documentos/{id}             → 204
  PATCH /api/documentos/{id}/status       → 200 DocumentoResponse (RF15 — header gerenciador)
  ```

- [ ] **Step 1: testes (`DocumentoApiTest`)** — helper do T4/T5: submissão RASCUNHO no Edital **com exigência obrigatória HISTORICO_ESCOLAR** (criar via API `/api/editais/{id}/exigencias`):
  1. `upload_pdf_valido_retorna201` → multipart PDF pequeno → 201, `$.statusValidacao == "PENDENTE"`, `$.tipoDocumento == "HISTORICO_ESCOLAR"`; GET metadados size 1.
  2. `upload_naoPdf_retorna400` → text/x-txt part em `arquivo` → 400 `ARQUIVO_INVALIDO`.
  3. `upload_tipoOutro_semDescricao_retorna400` → `tipoDocumento=OUTRO` sem `descricaoPersonalizada` → 400 `$.erro == "DESCRICAO_OBRIGATORIA"`.
  4. `upload_emSubmissaoEnviada_retorna409` → enviar a submissão primeiro → POST doc → 409 `STATUS_NAO_EDITAVEL` (RN03).
  5. `upload_5MB_retorna400` → array `new byte[5 * 1024 * 1024 + 1]` content-type PDF → 400 `ARQUIVO_MUITO_GRANDE`.
  6. `download_devolveMesmosBytes` → GET download → 200 com body igual ao enviado e header `Content-Disposition` com nome; após DELETE → 204 e GET download → 404.
  7. `header_naoDono_upload_retorna404`.
  8. `validacao_gerenciador_statusIncompletoSemObservacao_retorna400` → PATCH status `{statusValidacao:"INCOMPLETO"}` sem observação → 400 `OBSERVACAO_OBRIGATORIA`; com observação → 200 visível na listagem (RF15).
  9. `upload_emPendenteDocumentos_permite201_whiteTestRN03` → segunda submissão enviada e marcada `PENDENTE_DOCUMENTOS` pelo gerenciador → POST doc → **201** (caminho positivo da RN03).
  10. `upload_tipoDocumentoEnumInvalido_retorna400` → `tipoDocumento=RECEITUARIA` → 400.
- [ ] **Step 2: rodar e ver falhar.**
- [ ] **Step 3: implementar** camadas (armazenamento → service → controllers).
- [ ] **Step 4: rodar e ver passar** + suite completa (`./mvnw test -q`).
- [ ] **Step 5: commit**

```bash
git add -A src src/main/resources && git commit -m "feat(documentos): upload/download/delete RF09, validação RF15, RN03/RN11 e armaz. 5MB"
```

### Task 8: Evidência (C5) — Insomnia completo, Postman consolidado, roteiro de demo, README

**Files:**
- Rewrite (completo): `docs/insomnia/sgba-checkpoint3.insomnia.json`
- Modify: `docs/postman/sgba-checkpoint3.postman_collection.json` (consolidar fragmentos; manter `docs/postman/fragmentos` como esqueleto para os demais PRs? — não: torná-los redundantes **preenchendo-os** e deixando a collection como artefato final)
- Create: `docs/demo-checkpoint3.md` (roteiro < 10 min para arguição)
- Modify: `README.md` (seções 6/7: pastas novas do workspace, endpoints das Issues 5/6, tabela de códigos de erro da iteração)

**Checklist da coleção (ordem de pastas; nome " 5 · Submissões (Issue 5)") e requests (bodies completos):**
0. Fundação (3) | 1. Usuários (8) | 2. Alunos (7) | 3. Programas (14) — **reaproveitar os requests atuais** do export (copiar conteúdo aos mesmos `_id`s).
4. Editais (10; **pasta nova da Issue 4 que faltava**): listar/editais/{id}/formulario; criar edital válido; duplicado 409; janela invertida 400; upload/download PDF; perguntas POST/PUT/DELETE e exigências POST/DELETE (bodies exemplificados abaixo); ordem duplicada 409.
5. Submissões (18; Issue 5): criar rascunho Edital A (header aluno) → duplicada 409 → orientador inválido 400 → PUT respostas+termo → pendências 409 → enviar 200 (Edital A) → Edital C fora do prazo 409 → PATCH EM_ANALISE (header gerenciador) → PATCH PENDENTE_DOCUMENTOS com motivo → transição válida APROVADA → PATCH sem motivo 400 → PATCH com header aluno 400 → RASCUNHO→APROVADA 409 → GET detalhe / minhas / historico (4 eventos) → painel gerenciador (RF14) → snapshot (PUT /api/alunos/{usuarioAlunoId} altera CRA → GET submissão mantém `craMomento`).
6. Documentos (9; Issue 6): multipart upload Edital B (Histórico/Comprovante) → não-PDF 400 → OUTRO sem descrição 400 → RN03 409 → metadados 200 → download → delete 204 → RF15 PATCH gerenciador → observar `qtdDocumentos` em GET detalhe.
7. Notificações (5; Issue 6): inbox `somenteNaoLidas` → marcar lida (200) → idempotente (200 de novo) → filter unread = vazio → 404 de outro dono.

- Environment único `dev (compose + seed)`: variáveis das anteriores **+** `submissaoId=1`, `submissaoIdDuplicada=2`, `submissaoPainelEditalC=3`, `submissaoIdDocs=4`, `documentoId=1`, `notificacaoId=1`, `perguntaAId=1`, `exigenciaBHistorico`/etc. — descrição do env explica a política de atualizar ids das submissões criadas na demo em banco limpo.
- **Multipart no Insomnia v4:** request body `{ "mimeType": "multipart/form-data", "params": [{"name":"arquivo","type":"file","fileName":"documento.png"}, {"name":"tipoDocumento","value":"HISTORICO_ESCOLAR"}] }` com descrição "Anexe um PDF local no campo arquivo".
- Validar JSON: `jq empty docs/insomnia/sgba-checkpoint3.insomnia.json`.

- [ ] **Step 1: escrever** `docs/insomnia/…insomnia.json` (copiar textos/scenários dos 30 requests já existentes)
- [ ] **Step 2: preencher fragmentos Postman** (`fragmento-submissao.json`, `fragmento-documento.json`, `fragmento-notificacao.json`; criar `fragmento-edital.json` completo) e **consolidar** `sgba-checkpoint3.postman_collection.json` com info/variável `baseUrl` + pastas 0–7 em Postman v2.1 — validar com `jq empty`.
- [ ] **Step 3: escrever** `docs/demo-checkpoint3.md` — roteiro em 5 blocos (< 10 min): 1) subir infra (`docker compose up -d` + app dev + log do seed) · 2) provas C1/C2 (auditoria + reposit conceptível `./mvnw test`) · 3) fluxo completo Submissões (C3/C4 codes por step) · 4) Documentos (RF09/RF15) + Notificações · 5) coleção Insomnia como back-check. Citar dados do seed e headers.
- [ ] **Step 4: atualizar README.md** (seção 6: pastas 4–7; nova seção contratos Issues 5/6: tabela de rotas/status da iteração; nota da pill `X-Usuario-Id`).
- [ ] **Step 5: commit**

```bash
git add docs README.md && git commit -m "docs(c5): coleção Insomnia completa (Issues 1–6), Postman consolidado, demo-checkpoint3 e README"
```

### Task 9: Revisão geral — aterrar C1–C5 + suite + smoke + PR

**Files:**
- Create: `docs/auditoria-checkpoint3-final.md`
- (ninguém edita código aqui — só correções se a smoke test revelar bug)

- [ ] **Step 1: suite completa** — Run: `./mvnw test -q` → Expected: BUILD SUCCESS (total verde).
- [ ] **Step 2: smoke ao vivo** — `docker compose up -d` + `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` (outro shell) e rodar fluxo mínimo por `curl`: POST rascunho (header aluno) → PUT respostas → POST enviar → PATCH status (header gerenciador) → GET história → POST multipart de doc → GET inbox do aluno. Esperado: 201/200/200/200 sem 500.
- [ ] **Step 3: escrever `docs/auditoria-checkpoint3-final.md`** — tabelas:
  - **C1**: entidades↔DER (link para auditoria da Issue 1, `Submissao.programa` removida, uniques ativas);
  - **C2**: os **11` repositories** com consultas de negócio (módulo ↔ método ↔ RN);
  - **C3**: RNs implementadas (RN01-RN12 com arquivo/classe/método + código de erro + onde a coleção prova);
  - **C4**: mapa completo rotas → verbo/status (todas as issues) → e formato `{status, erro, mensagem}` via handler base;
  - **C5**: coleção Insomnia 100%, Postman consolidado, `demo-checkpoint3.md`, seed;
  - **foras de escopo do cp3→cp4** (auth, Bean Validation, etc.) explicadas em 5 linhas.
- [ ] **Step 4: PR** — push + `gh pr create` com body: resumo por camada, mapa RN→arquivo, evidência (rotas demonstáveis), `Closes #5` e `Closes #6`, decisões documentadas (lista dos Global Constraints) — **não** mergear; aguardar revisão humana.

```bash
git push -u origin feature/issues-5-6-final-checkpoint3
gh pr create --title "feat: Issues 5+6 — Submissões, Documentos, Notificações + evidência final do Checkpoint 3" --body-file /tmp/opencode/pr-body.md
```

- [ ] **Step 5: commit final da auditoria**

```bash
git add docs/auditoria-checkpoint3-final.md && git commit -m "docs(cpc): auditoria final C1–C5 do Checkpoint 3"
```

---

## Cobertura final esperada (resumo executável da revisão)

| Critério | Delidade desta iteração |
|---|---|
| C1 Entidades ↔ DER | já ok (Issue 1) — reafirmado na auditoria final |
| C2 Repositories | **11 interfaces** com consultas de negócio (5 entram agora) |
| C3 Service/RNs | RN02–RN05, RN07–RN12 + RF16 tabelado; códigos/mensagens do spec |
| C4 Controllers | rotas `/api/submissoes*`, `/api/documentos*`, `/api/notificacoes*`, `/api/gerenciador/submissoes` com 200/201/204/400/404/409 + Location/attachment |
| C5 Evidência | Insomnia completo (pastas 0–7), Postman consolidado, demo-checkpoint3.md, README, smoke a vivo |

---

## Suplemento de execução (rulings pós-plano)

- **T9: Ruling:** em `mudarStatus`, RN04 (motivo ausente → 400) passa a checar **antes** da tabela RF16 (409): o roteiro de demo prova `MOTIVO_OBRIGATORIO` independentemente do status corrente (o smoke mostrou que PATCH repetido em PENDENTE_DOCUMENTOS sem motivo dava 409, confundindo o bloqueio esperado). Custo se errado: "mesmo status sem motivo" responde 400 em vez de 409 — coerente com D2 (argumento inválido) e com os testes existentes.
