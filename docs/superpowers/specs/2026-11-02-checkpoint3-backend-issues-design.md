# Design — Checkpoint 3: Back-end até Controller (SGBA)

**Data:** 2026-11-02 · **Repositório:** `sgba-backend` · **Equipe:** 4 colaboradores
**Fontes normativas:** `requisitos.md` + `docs/der.dbml` (repo `sgba-frontend`) e o PDF de avaliação da disciplina.

---

## 1. Objetivo e por quê

O Checkpoint 3 vale **20% da nota** e exige, por critério:

| # | Critério de avaliação | Como será atendido |
|---|---|---|
| C1 | Entidades JPA correspondem ao DER validado no checkpoint 2 | Issue 1 — auditoria das 11 entidades vs `der.dbml` v3 (há divergências reais, ver §2.4) |
| C2 | Repository implementado para as entidades core | Issues 1–6: `repository` Spring Data para as entidades do módulo de cada issue |
| C3 | Service contém regra de negócio real | Cada módulo tem RNs reais do DRS (protocolo, unicidade, janela de envio, snapshot, histórico, transições de status — §6, Issue 5) |
| C4 | Controller com endpoints REST coerentes (verbos/status HTTP corretos) | Contrato REST por módulo (§6) com verbos e status definidos + base de tratamento de erro (Issue 1) |
| C5 | Evidência de funcionamento (CRUD via Postman/Insomnia ou demo ao vivo) | Coleção Postman compartilhada no repo + seed de dados + checklist de demo (Issues 1 e 6) |

Este documento é a fonte única para as **6 issues** do checkpoint. Cada seção da §6 é uma issue pronta para ser copiada para o GitHub.

## 2. Escopo

### 2.1 Dentro do escopo
- Auditoria/correção das entidades JPA para ficar em 1:1 com o `der.dbml` (v2 → v3 justificado, §9.1) validado no checkpoint 2.
- Repositories (Spring Data JPA) das entidades core.
- DTOs (records) de request/response por módulo, mapeamento manual (sem MapStruct).
- Services com as RNs listadas por módulo (§6).
- Controllers REST com verbos e status HTTP corretos (201/200/204/400/404/409).
- Config de infraestrutura (PostgreSQL via Docker Compose, profiles de dev/teste).
- Base mínima de tratamento de erro (o tratamento completo e centralizado é do checkpoint 4).
- Seed de dados de desenvolvimento + coleção Postman + checklist de demonstração.

### 2.2 Fora do escopo (futuros checkpoints)
- ❌ Spring Security / RBAC / autenticação — **Checkpoint 4**. Interim: header `X-Usuario-Id` (§4.1).
- ❌ Validação abrangente com Bean Validation + `@ControllerAdvice` completo — **Checkpoint 4** (a base mínima da Issue 1 não consome os critérios do cp4).
- ❌ Geração de PDF (RF10), exportação CSV (RF18) — issues futuras.
- ❌ Integração com o front-end — **Checkpoint 5**.
- ❌ Migrations (Flyway) — ddl-auto `update` em dev é aceito para o checkpoint (RNF04).

### 2.3 Anti-metas (o que faria o checkpoint falhar)
- Entidades divergentes do DER (ex.: `submissoes.programa_id` que **não existe** no DER validado).
- Controller com regra de negócio (o corpo com `if` de negócio deve morar no Service).
- Status HTTP aleatórios (200 para criação, 200 para recurso inexistente etc.).
- Repository sem uso ou entidade sem consultas coerentes com o módulo.

## 2.4 Divergências entidades × DER já identificadas (insumo da Issue 1)

Levantadas por inspeção em 02/11 (código atual × `der.dbml`):

1. `Submissao` tem FK `programa_id` (`@ManyToOne ProgramaBolsa programa`) — o DER validado **removeu** essa FK: comentário no DBML §submissoes: `edital_id bigint [not null] // FK -> editais.id (troca de programa_id)`. **Remover `programa` da entidade**; programa passa a ser obtido via `submissao.edital.programa`.
2. Constraints de unicidade ausentes nas entidades (DBML exige todas):
   - `Editais`: `uk_edital_numero_ano (numero_edital, ano)`
   - `PerguntasEdital`: `uk_pergunta_ordem (edital_id, ordem_exibicao)`
   - `ExigenciasEdital`: `uk_exigencia_edital_tipo (edital_id, tipo_documento)`
   - `Submissoes`: `uk_aluno_edital (aluno_id, edital_id)`
   - `RespostasSubmissao`: `uk_resposta_unica (submissao_id, pergunta_id)`
   (`Usuario.email`, `Aluno.cpf/matricula`, `Submissao.protocolo` já têm `unique`.)
3. `Aluno.matricula`: DBML define `not null` — verificar e ajustar na entidade.
4. Verificação completa campo-a-campo de todas as 11 entidades e 7 enums vs DBML (tabela comparativa no PR da Issue 1).
5. Contrato de `exigencias` não inclui `descricaoPersonalizada` — essa coluna só existe em `documentos_anexo` (RN11). Issue 4 ajustada para não pedir esse campo.

## 3. Restrições

- Java 21, Spring Boot 4.x, PostgreSQL (RNF01/RNF04) — ambiente e skeleton já existem no repo.
- Camada conforme RNF02: `controller → service → repository`, DTOs entre camadas, entidades JPA.
- Estilo do repo: Lombok, pacotes por camada já criados (`entity`, `repository`, `service`, `controller`, `dto`, `exception`, `config`), base `br.edu.upe.sgba`.
- Prazo acadêmico: checkpoint avalia CRUD funcional demonstrável, não produção.

## 4. Decisões técnicas (chamadas feitas por mim; revisáveis)

| Decisão | Valor | Motivo |
|---|---|---|
| **D1. "Usuário corrente" sem auth** | Controllers que precisam do usuário leem o header `X-Usuario-Id` e o Service valida a existência/atuação (aluno/professor). | Não há auth no cp3; isolamento (RN06) continua testável. No cp4 troca-se por `@AuthenticationPrincipal` sem quebrar services. |
| **D2. Tratamento de erro mínimo (transição cp3→cp4)** | `@RestControllerAdvice` base com handlers para: recurso não encontrado → `404`, conflito de unicidade/regra → `409`, argumento inválido → `400`, com body `ErrorResponse(record)` por código. `RegraNegocioException` **carrega status HTTP** (default 409; para regras de "argumento inválido" usa-se 400 — ex.: RN01, RN04, janelas). | O critério C4 exige status coerentes. O cp4 completa com Bean Validation robusto e handlers de auth — a base não disputa os critérios do cp4. |
| **D3. DTOs** | Java `record`s com sufixos `Request`/`Response`, no pacote `dto`, mapeamento por métodos estáticos `from(...)`/`toEntity(...)`. | Sem dependência nova (MapStruct); Java 21; explícito e didático para arguição do cp5. |
| **D4. Paginação** | Listas de catálogo usam `Pageable` (ex.: `GET /api/programas?page=0&size=10&sort=titulo,asc`). | Coerência REST e usabilidade direta no front (cp5). |
| **D5. IDENTITY + ddl-auto** | `GenerationType.IDENTITY` (já nas entidades); dev usa `ddl-auto=update` contra PG Docker. | Simples, adequado ao checkpoint; Flyway fica para depois. |
| **D6. Geração de protocolo (RN02)** | `protocolo = #AAAA-XXXX` com contador no ano (`count(*) where protocolo like 'AAAA-%'`) gerado em transação + verificação de `IllegalArgumentException` de constraint única (máx. 5 tentativas). | Sem tabelas de sequência extras; corrida rara no escopo acadêmico; observação sobre alternativa (tabela de sequência) na Issue 5. |
| **D7. Armazenamento de arquivos (RNF06)** | Diretório local configurável `sgba.storage.dir` (default `./storage`); entidades gravam caminho relativo. | Elimina dependência de bucket; suficiente para demo. |
| **D8. Sem testes automatizados como critério** | Testes de repository/services são opcionais e bem-vindos, mas a evidência cobrada é a coleção Postman. | O critério C5 é "CRUD via Postman/Insomnia ou demo ao vivo". |
| **D9. Convenção de PR** | Branch `feature/issue-N-<tema>`; 1 issue = 1 PR; PR pequeno revisável (< ~600 linhas ideal). | Integração previsível entre pares de módulos. |

### 4.1 Padrão temporário `X-Usuario-Id` (padrão de todas as issues)
```
Header: X-Usuario-Id: <id do usuario>
Em endpoints de "minhas?" (RN06) → valida existência e que o recurso pertence ao usuário do header.
No Checkpoint 4: substituído por credencial autenticada; assinaturas de Service não mudam.
```

### 4.2 Padrão de erro temporário (D2)
```json
{ "status": 409, "erro": "SUBMISSAO_DUPLICADA", "mensagem": "Aluno já possui submissão para este edital" }
```

## 5. Fatiamento, dependências e distribuição sugerida

```
Issue 1 (Fundação) ─┬─► Issue 2 (Usuários/Alunos) ─┐
                    ├─► Issue 3 (Programas) ──► Issue 4 (Editais/Perguntas/Exigências) ─┤
                    │                                                                  ├─► Issue 5 (Submissões) ─► Issue 6 (Docs/Notif/Evidência final)
                    └──────────────────────────────────────────────────────────────────┘
```

- Issues 2–6 só tocam arquivos do seu próprio módulo → **sem conflito de arquivos entre pessoas**.
- Issue 5 depende das entidades dos módulos 2–4 **apenas para integração em runtime** (o PR pode ser escrito em paralelo — o seed da Issue 1 fornece Programa/Edital/Usuários de exemplo).
- Issue 6 depende de Issue 5 para os fluxos de upload/histórico; as partes de Notificações podem ser escritas antes.

**Distribuição sugerida (4 pessoas — ajustável):**

| Pessoa | 1ª metade | 2ª metade |
|---|---|---|
| P1 | Issue 1 (fundação) | Apoio em review + Issue 6 (evidência/demo) |
| P2 | Issue 3 (programas) | Issue 4 (editais) |
| P3 | Issue 2 (usuários/alunos) | Issue 5 (submissões — módulo mais pesado; iniciar estudo do ciclo de estados desde a semana 1) |
| P4 | Apoio na Issue 1 (auditoria de entidades vs DER) | Issue 6 (documentos/notificações + montagem da evidência) |

**Esforço estimado (bruto):** I1 ≈ 2–3 d · I2 ≈ 3–4 d · I3 ≈ 3 d · I4 ≈ 4–5 d · I5 ≈ 6–8 d · I6 ≈ 5–6 d. Com as duas fases, ~2 semanas em paralelo (estimativa acadêmica).

---

## 6. As issues

> Cada bloco abaixo é uma issue pronta para o GitHub (título, labels, corpo). Base de rótulos: `checkpoint-3`, `backend` + rótulo do módulo.

### ── ISSUE 1 ── Fundação: infraestrutura, auditoria de entidades vs DER, erros base, seed e esqueleto da evidência

**Labels:** `checkpoint-3`, `backend`, `fundacao` · **Depende de:** nenhuma · **Bloqueia:** todas.
**Objetivo:** preparar o terreno técnico e saldar imediatamente o critério C1 (entidades ↔ DER) e parte do C5 (infra de evidência).

**Checklist de tarefas**
- [ ] `compose.yaml` com `postgres:16` (db `sgba`, user `sgba`, senha `sgba`, porta 5432, healthcheck, volume).
- [ ] Configuração por profile (`application.yml` ou `.properties`):
  - `dev`: PostgreSQL host local, `ddl-auto=update`, `show-sql=true`;
  - `test`: H2 in-memory (opcional);
  - propriedade `sgba.storage.dir` (D7).
- [ ] **Auditoria de entidades (C1):** construir tabela comparativa campo-a-campo das 11 entidades + 7 enums vs `der.dbml` **v3** (decisão §9) e aplicar divergências de §2.4:
  - remover `Submissao.programa` (FK vs v3); `Submissao.protocolo` fica **nullable** (nulo em RASCUNHO, preenchido no `enviar` — ver §9.1);
  - `Edital.caminhoArquivoPdf` fica **nullable/optional** (JSON cria, upload depois preenche — §9.1);
  - 5 constraints de unicidade (§2.4.2); `Aluno.matricula not null`;
  - demais divergências que a auditoria campo-a-campo revelar.
- [ ] **DER v3 no repo frontend (§9.1):** atualizar `sgba-frontend/docs/der.dbml` para v3 (mudanças justificadas inline: protocolo preenchido no envio; caminho_arquivo_pdf preenchido no upload), registrar em `der-mudancas.md` e anexar tabela v2→v3 no PR desta issue.
- [ ] **Erros base (D2):**
  - `exception/RecursoNaoEncontradoException.java`;
  - `exception/RegraNegocioException.java` (carrega status HTTP; default 409 — D2);
  - `exception/ErrorResponse.java` (record: `status`, `erro`, `mensagem`);
  - `config/GlobalExceptionHandler.java` (`@RestControllerAdvice`) com mapeamentos 404/409/400.
- [ ] **Seed de dev** (`config/DevSeedData`, `@Profile("dev")`, `CommandLineRunner`) criando:
  - 3 usuários: 1 aluno, 1 professor, 1 gerenciador (e-mail `@upe.br`, senha qualquer — sem auth no cp3);
  - perfil `Aluno` completo (dados de exemplo);
  - 1 `ProgramaBolsa` + **4 `Editais`** para servir às demos (ver abaixo) + 3 `PerguntaEdital` (1 opcional) + 2 `ExigenciaEdital`:
    - **Edital A** — janela aberta, 2 perguntas (1 obrigatória), 2 exigências **opcionais** → usado pelo fluxo feliz da Issue 5 (envio possível sem documentos);
    - **Edital B** — janela aberta, exigências **obrigatórias** → usado pela Issue 6 para demonstrar RN12 (envio incompleto → com docs → envio OK);
    - **Edital C** — janela encerrada (passada) → usado pela Issue 5 para demonstrar RN05 (409 `FORA_DO_PRAZO`);
    - **Edital D** — para infraestrutura de perguntas/exigências (Issue 4) e duplicatas.
  - **Importante:** este seed é o *contrato público* que as Issues 2–6 usam em suas demos — não mudar os IDs sem avisar no PR.
- [ ] **Esqueleto da evidência (C5):** pasta `docs/postman/` com **fragmentos por módulo** (`fragmento-usuario.json`, `fragmento-programa.json`, ... — evita conflito de merge entre PRs paralelos) e uma coleção vazia `sgba-checkpoint3.postman_collection.json` (variável `baseUrl`); a Issue 6 consolida tudo no arquivo único final; no `README.md`: como subir `docker compose up -d`, como rodar a app, como recarregar o seed.
- [ ] Convenções do projeto documentadas no PR: D1–D4, D6–D9 + padrão de nomes de DTOs.

**Contrato REST (novo):** nenhum endpoint de produto nesta issue (apenas infra/erros).

**Critérios de aceite**
- `docker compose up -d` + `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` = app sobe, seed criado (verificar via `\dt` no PG / logs).
- `GET /api/inexistente` → 404 com body `ErrorResponse` (rota de teste aceita para provar o handler).
- Tabela comparativa entidades×DER v3 anexada ao PR.
- `Submissao` sem FK `programa` e com `protocolo` nullable; 5 constraints de unicidade presentes; `Edital.caminhoArquivoPdf` nullable.

**Fora do escopo:** endpoints de produto (Issues 2–6); validação robusta (cp4); Flyway.

---

### ── ISSUE 2 ── Módulo Usuários e Alunos (CRUD + RN01)

**Labels:** `checkpoint-3`, `backend`, `modulo-usuarios` · **Depende de:** Issue 1 · **Único módulo sem dependência entre módulos.**
**Objetivo:** CRUD de usuários (identidade) e de perfil estendido de aluno (1:1 com `MapsId`), provendo base para orientador (RN10) e para todas as FKs de submissão.

**Checklist de tarefas**
- [ ] `repository/UsuarioRepository.java`: `findByEmail`, `existsByEmail`, `findByFuncao`, `existsByFuncao(Usuario)` p/ validação de professor (RN10).
- [ ] `repository/AlunoRepository.java`: `findById` (herda `usuario_id`), `existsByCpf`, `existsByMatricula`.
- [ ] DTOs (`dto/`): `UsuarioCreateRequest`, `UsuarioResponse`, `AlunoUpsertRequest` (endereço embutido), `AlunoResponse`, `ProfessorResponse` (id, nome, email).
- [ ] `service/UsuarioService.java`:
  - RN01: e-mail deve terminar em `@upe.br` → `RegraNegocioException` com status **400** `EMAIL_INSTITUCIONAL_INVALIDO`;
  - e-mail único → 409 `EMAIL_JA_CADASTRADO`;
  - criação de usuário com `funcao` do enum (não aceitar valores fora do enum);
  - `listarProfessores()` — emite exceção se nenhum professor; usada pelo wizard (RF07 passo 2);
  - **helper `exigeFuncao(usuarioId, Funcao...)`**: valida existência do usuário e função; lança 400 `PERMISSAO_INSUFICIENTE`. Usado pelas Issues 5 e 6 nos endpoints críticos (decisão §9.2).
- [ ] `service/AlunoService.java`: perfil estendido; CPF/matricula únicos → 409; atualização parcial aceita; 1:1 com `Usuario` obrigatória (criar aluno exige `usuarioId` existente e sem perfil de aluno ainda → 409 `ALUNO_JA_EXISTENTE`).
- [ ] `controller/UsuarioController.java` + `controller/AlunoController.java`.

**Contrato REST**

| Verbo | Rota | Entrada (exemplo) | Status |
|---|---|---|---|
| GET | `/api/usuarios?funcao=PROFESSOR` | — | 200 (lista `ProfessorResponse`) |
| GET | `/api/usuarios/{id}` | — | 200 / **404** |
| POST | `/api/usuarios` | `{ "nome": "...", "email": "aluno@upe.br", "senha": "...", "funcao": "ALUNO" }` | **201** + Location / **400** (RN01 — e-mail fora do domínio, enum inválido) / **409** (e-mail duplicado) |
| PUT | `/api/usuarios/{id}` | payload parcial | 200 / 404 |
| GET | `/api/alunos/{usuarioId}` | — | 200 / **404** |
| POST | `/api/alunos` | `{ "usuarioId": 1, "cpf": "...", "matricula": "...", "craAtual": 8.5, "endereco": { ... } }` | **201** / **409** (cpf/matricula esgotado, usuário sem perfil) |
| PUT | `/api/alunos/{usuarioId}` | como POST sem `usuarioId` | 200 / 404 |

**Regras de negócio cobertas:** RN01 (e-mail `@upe.br`), unicidades (email, cpf, matrícula), suporte à RN10 (listagem de professores).

**Critérios de aceite (Postman — anexar pasta "Usuários/Alunos" na coleção)**
- CRUD completo demonstrado; criação com e-mail fora do domínio → 400 com `ErrorResponse`; duplicatas (e-mail/cpf/matrícula) → 409; 201 com header `Location`.
- Aluno criado junto a usuário com `MapsId` correto (mesmo `id`).

**Fora do escopo:** hash de senha (cp4), autenticação/login (cp4), `DELETE /api/usuarios/{id}` (sensitive: fica fora deste checkpoint — decisão documentada no PR).

---

### ── ISSUE 3 ── Módulo Programas de Bolsa (CRUD catálogos + visibilidade por datas)

**Labels:** `checkpoint-3`, `backend`, `modulo-programas` · **Depende de:** Issue 1.
**Objetivo:** ciclo completo CRUD de catálogo de programas (RF03) + regra de visibilidade por datas (RF05/RN05 visibilidade geral), provando "Service com regra de negócio real" fora do módulo de submissões.

**Checklist de tarefas**
- [ ] `repository/ProgramaBolsaRepository.java`: `findByCategoria`, `findByTituloContainingIgnoreCase`, consulta de `abertos` (datas efetivas), `findAll(Pageable)`.
- [ ] DTOs: `ProgramaUpsertRequest`, `ProgramaResponse` (com `status` do enum e datas), `ProgramaCatalogoResponse` (card público do catálogo).
- [ ] `service/ProgramaBolsaService.java`:
  - regra de datas: `dataLimite > dataAbertura` → se violar, 400 `PERIODO_INVALIDO`;
  - `listarAbertos()`: calculados por datas efetivas (agora entre abertura/limite) — visão pública do catálogo;
  - filtro combinável categoria|busca textual|status;
  - `encerrar(id)`: transição manual de status (enum `StatusPrograma`) — controle do gerenciador (RF03).
- [ ] `controller/ProgramaBolsaController.java` (paginado conforme D4).

**Contrato REST**

| Verbo | Rota | Nota | Status |
|---|---|---|---|
| GET | `/api/programas` (filtros: `categoria`, `q`, `status`, page/size/sort) | lista completa paginada (telas gerenciador, RF03/RF14) | 200 |
| GET | `/api/programas/abertos` | catálogo público do aluno (RF05) — regra por datas | 200 |
| GET | `/api/programas/{id}` | — | 200 / **404** |
| POST | `/api/programas` | `{ "titulo": "PIBIC", "categoria": "INICIACAO_CIENTIFICA", "valorMensal": 900.00, "vagas": 10, "periodoLetivo": "2026.1", "dataAbertura": "...", "dataLimite": "...", "status": "ABERTO", "descricao": "..." }` | **201** + Location / **400** (datas inválidas/enum) |
| PUT | `/api/programas/{id}` | atualização completa | 200 / 404 / 400 |
| POST | `/api/programas/{id}/encerrar` | ação do gerenciador (desativar, RF03) | 200 / 404 / **409** se já ENCERRADO |

**Regras de negócio cobertas:** validação de janela do programa; visibilidade por datas no `abertos`; status manual (RF03).

**Critérios de aceite (Postman — pasta "Programas")**
- CRUD completo; POST com `dataLimite < dataAbertura` → 400; `/abertos` reflete apenas programas na janela; encerrar duas vezes → 409 na segunda.
- Seed da Issue 1 aparece em `/abertos` (integra o catálogo do aluno).

**Fora do escopo:** upload de PDF de edital (Issue 4); `@PreAuthorize` (cp4).

---

### ── ISSUE 4 ── Módulo Editais, Perguntas e Exigências (formulário dinâmico por edição)

**Labels:** `checkpoint-3`, `backend`, `modulo-editais` · **Depende de:** Issue 3 (FK programa) e Issue 1.
**Objetivo:** CRUD de editais vinculados a programas + gestão aninhada de perguntas e exigências (RF04), com as cinco uniques por edição — entrada obrigatória para o ciclo de submissões.

**Checklist de tarefas**
- [ ] `repository/EditalRepository.java`: `findByProgramaId`, `findByNumeroEditalAndAno`, `findById` com fetch (join) de perguntas e exigências.
- [ ] Repositories `PerguntaEditalRepository`, `ExigenciaEditalRepository` (consultas por `editalId`, ordem exibição).
- [ ] DTOs: `EditalUpsertRequest` (dados base), `EditalComFormularioResponse` (edital + perguntas/exigências ordenadas), `PerguntaRequest/Response`, `ExigenciaRequest/Response`.
- [ ] `service/EditalService.java`:
  - regra de janela: `dataFimSubmissoes > dataInicioSubmissoes` → 400 `JANELA_INVALIDA` (RN05-a);
  - unicidade `numero_edital + ano` → 409 `EDITAL_DUPLICADO` (uk do DER);
  - CRUD de perguntas com unicidade de `ordem` por edital (409 `ORDEM_JA_USADA`) e ordenação na resposta;
  - CRUD de exigências com unicidade de `tipo_documento` por edital (409 `TIPO_JA_EXIGIDO`);
  - PDF do edital: upload multipart para `sgba.storage.dir` (D7) com `caminhoArquivo` salvo — tamanho máx. configurável (default 10MB), aceita `application/pdf` (verificação mínima de content-type).
- [ ] Controllers `EditalController`, `PerguntaEditalController`, `ExigenciaEditalController` (rota aninhada sob `/api/editais/{editalId}/...`).

**Contrato REST**

| Verbo | Rota | Nota | Status |
|---|---|---|---|
| GET | `/api/editais?programaId=` | lista (RF05) | 200 |
| GET | `/api/editais/{id}` | ficha + janela (RN05 texto para o wizard) | 200 / **404** |
| GET | `/api/editais/{id}/formulario` | perguntas (ordenadas) + exigências (wizard RF07 passo 3/4, RF06) | 200 / 404 |
| POST | `/api/editais` | `{ "programaId": 1, "numeroEdital": "08", "ano": 2026, "dataInicioSubmissoes": "...", "dataFimSubmissoes": "..." }` | **201** / **409** (duplicado) / **400** (janela/enum) |
| PUT | `/api/editais/{id}` | dados base | 200 / 404 |
| POST | `/api/editais/{id}/arquivo` | multipart `file` | 201 (caminho salvo) / 400 (tipo/tamanho) / 404 |
| GET | `/api/editais/{id}/arquivo` | download do PDF oficial | 200 (`application/pdf`) / 404 |
| POST | `/api/editais/{id}/perguntas` | `{ "enunciado": "...", "obrigatoria": true, "ordemExibicao": 1 }` | **201** / **409** (ordem) / 404 (edital) |
| PUT | `/api/editais/{editalId}/perguntas/{perguntaId}` | — | 200 / 404 |
| DELETE | `/api/editais/{editalId}/perguntas/{perguntaId}` | — | **204** / 404 |
| POST / PUT / DELETE | `/api/editais/{id}/exigencias[...]` | `{ "tipoDocumento": "HISTORICO_ESCOLAR", "obrigatorio": true }` (sem `descricaoPersonalizada` — ver §2.4.5; RN11 fica na **criação** de documentos, Issue 6) | idem perguntas |

**Regras de negócio cobertas:** uniques por edição (DER); janela de submissões (RN05); base para RN11/RN12; armazenamento de arquivos (RNF06).

**Critérios de aceite (Postman — pasta "Editais")**
- CRUD de edital + perguntas + exigências demonstrado; duplicata de nº/ano → 409; ordem repetida → 409; tipo repetido → 409; janela invertida → 400; upload/download do PDF funciona.
- `/formulario` usado nas demos da Issue 5 (fornecer id de edital com seed).

**Fora do escopo:** validação de conteúdo do arquivo além de content-type/tamanho (cp4); `@PreAuthorize` (cp4).

---

### ── ISSUE 5 ── Módulo Submissões — ciclo de vida (rascunho → envio → avaliação) ★ módulo central

**Labels:** `checkpoint-3`, `backend`, `modulo-submissoes`, prioridade `alta` · **Depende de:** Issues 1–4 em runtime (o PR pode ser iniciado em paralelo usando o seed) + helper `exigeFuncao` da Issue 2 (§9.2).
**Objetivo:** o coração do domínio: criar rascunho (RN08), completar respostas ao formulário (RF07.3), enviar (RN02/RN05/RN07/RN09/RN12), timelines (RF12) e transição de status pelo gerenciador (RF16 simplificada, sem auth). É onde "Service com regra de negócio real" fica mais explícito (C3).

**Checklist de tarefas**
- [ ] Repositories: `SubmissaoRepository` (`findByAlunoId`, `existsByAlunoIdAndEditalId`, `findByProtocolo`), `RespostaSubmissaoRepository`, `HistoricoStatusRepository` (`findBySubmissaoIdOrderByDataHoraDesc`).
- [ ] DTOs: `SubmissaoRascunhoRequest` (editalId, tituloProjeto, projetoResumo, planoTrabalho, orientadorId), `RespostaUpsertRequest` (perguntaId, respostaTexto), `SubmissaoEnviadaResponse`, `SubmissaoPainelResponse` (protocolo, edital/programa, status, dataEnvio, qtdDocumentos), `HistoricoResponse`, `DecisaoRequest` (novoStatus, motivo).
- [ ] `service/SubmissaoService.java` — regras reais:
  - **RN08** rascunho unicidade por edital: `existsByAlunoIdAndEditalId` → 409 `SUBMISSAO_DUPLICADA` (leitura inicial + guard no save);
  - **RN10** orientador válido: se `orientadorId` informado → usuário com `funcao=PROFESSOR`, senão 400 `ORIENTADOR_INVALIDO`;
  - **RN03** só `RASCUNHO` (ou `PENDENTE_DOCUMENTOS` p/ documentos, Issue 6) é editável pelo aluno → 409 `STATUS_NAO_EDITAVEL`;
  - **RN05** janela de envio: `enviar` só com `now` entre `dataInicio/fimSubmissoes` do edital → 409 `FORA_DO_PRAZO`;
  - **RN02** protocolo `#AAAA-XXXX` anual (D6) na transição de envio;
  - **RN09** snapshot do mérito no envio: copia `curso/periodoAtual/craAtual/unidadeAcademica` do perfil do aluno → `cursoMomento/periodoMomento/craMomento/unidadeMomento`, `dataEnvio=now`, status → `RECEBIDA`;
  - **RN12** completude do envio: `termoAceite=true`; todas as `perguntas.obrigatoria` do edital respondidas; todas as `exigencias.obrigatorio` com documento anexado → senão 409 `ENVIO_INCOMPLETO` (lista dos faltantes na mensagem);
  - **RN07** histórico append-only: toda mudança de status insere `HistoricoStatus` (statusAnterior, statusNovo, observacao, dataHora, usuario) na MESMA transação;
  - **RF16** transições válidas (tabela abaixo) + **exige função GERENCIADOR** para mudar status (§9.2, helper da Issue 2 via `X-Usuario-Id`) → transição inválida 409 `TRANSICAO_INVALIDA` (RN04: motivo obrigatório para `PENDENTE_DOCUMENTOS`/`INDEFERIDA` → 400 `MOTIVO_OBRIGATORIO`);
  - **RN06** isolamento D1: `minhas`/detalhe/timeline conferem `X-Usuario-Id` com o dono do recurso — OU função `GERENCIADOR` (isenção necessária ao RF15, decisão §9.2); caso contrário **404** (não vazar existência).

**Máquina de estados (RF16):**

```
RASCUNHO ──enviar──► RECEBIDA ──► EM_ANALISE ──┬─► APROVADA (terminal)
                                              ├─► INDEFERIDA (terminal)
                                              └─► PENDENTE_DOCUMENTOS ──(docs reenviados/validados)─┬─► EM_ANALISE
                                                                                                   ├─► APROVADA
                                                                                                   └─► INDEFERIDA
```

- [ ] `service/NotificacaoPort` (interface em `service/port` + implementação em Issue 6): hook de notificação chamado nas transições — Issue 5 entrega a interface + no-op default, Issue 6 conecta (evita acoplamento entre módulos enquanto ambos evoluem).
- [ ] `controller/SubmissaoController.java` (+ rotas `/historico`).

**Contrato REST**

| Verbo | Rota | Corpo / nota | Status |
|---|---|---|---|
| POST | `/api/submissoes` | rascunho (header `X-Usuario-Id` do aluno) | **201** + Location / **409** (RN08) / **400** (RN10/enum/edital inexistente→404) |
| GET | `/api/submissoes/{id}` | — | 200 / **404** (+404 se não é do usuário — RN06) |
| PUT | `/api/submissoes/{id}` | atualiza rascunho + `respostas[]` + `termoAceite` (só RASCUNHO — RN03) | 200 / 404 / **409** |
| POST | `/api/submissoes/{id}/enviar` | valida RN05+RN12, faz RN02+RN09+RN07 | **200** (`protocolo`, `dataEnvio`, `status=RECEBIDA`) / 404 / **409** (fora do prazo/incompleto/transição) |
| GET | `/api/submissoes/minhas` | header do aluno; lista painel (RF11) | 200 |
| GET | `/api/submissoes/{id}/historico` | timeline RF12 (append-only) | 200 / 404 |
| PATCH | `/api/submissoes/{id}/status` | `DecisaoRequest { "novoStatus": "PENDENTE_DOCUMENTOS", "motivo": "Falta comprovante..." }` — **requer função GERENCIADOR** (header + helper da Issue 2, §9.2) | 200 / 404 / **409** (transição) / **400** (motivo ausente — RN04; função errada — §9.2) |
| GET | `/api/gerenciador/submissoes?filtro` *(opcional/stretch)* | lista com filtros por status/programa/edital (RF14) | 200 |

**Regras de negócio cobertas:** RN02, RN03 (parte), RN04, RN05, RN06 (via D1), RN07, RN08, RN09, RN10, RN12; base para RF14/RF16/RF17.

**Critérios de aceite (Postman — pasta "Submissões", fundada no seed)**
- Fluxo completo em ordem: criar rascunho → PUT respostas+termoAceite → `enviar` → `PATCH status EM_ANALISE` → `PENDENTE_DOCUMENTOS` com motivo → verificar `GET historico` mostrando os 4 eventos com statusAnterior/Novo/observação/dataHora.
- Demonstração dos bloqueios: rascunho duplicado (409), janela fechada (409 — Edital C com janela passada no seed), envio sem resposta obrigatória (409 com lista de pendências), transição `RASCUNHO→APROVADA` direta (409), motivo ausente (400), mudança de status com header de aluno (400 `PERMISSAO_INSUFICIENTE`, §9.2).
- Snapshot conferível: alterar perfil do aluno depois do envio e conferir que a submissão mantém os campos `*_momento` (RN09).
- (Se possível, orientador inválido → 400.)

**Fora do escopo:** notificações reais (Issue 6), reenvio de documentos na pendência (Issue 6), justificativa integral com validação de Bean (cp4), geração de PDF (futura).

---

### ── ISSUE 6 ── Módulos Documentos + Notificações e evidência final (Postman/demo)

**Labels:** `checkpoint-3`, `backend`, `modulo-documentos`, `evidencia` · **Depende de:** Issues 1 e 5 (pode começar pelas Notificações antes).
**Objetivo:** anexos de documentos (multipart + download + validação do gerenciador, RF09/RF15), inbox de notificações (RF17) e montar a **evidência oficial** do checkpoint (C5).

**Checklist de tarefas**
- [ ] Repositories: `DocumentoAnexoRepository` (`findBySubmissaoId`, `findBySubmissaoIdAndTipoDocumento`), `NotificacaoRepository` (`findByDestinatarioId`, `findByDestinatarioIdAndLidaEmIsNull`, `ordenar por criadoEm desc`).
- [ ] DTOs: `DocumentoUploadResponse` (payload multipart; resposta devolve os metadados salvos), `DocumentoResponse`, `DocumentoValidacaoRequest` (`statusValidacao`, `observacaoValidacao`), `NotificacaoResponse`, param `somenteNaoLidas`.
- [ ] `service/DocumentoAnexoService.java` + `service/armazenamento/ArquivoStorageService` (disk: save/stream — D7):
  - **validação de papel (§9.2)**: o endpoint de validação do documento exige função GERENCIADOR via `UsuarioService.exigeFuncao` (Issue 2);
  - **RN03**: upload/substituição/exclusão apenas com submissão em `RASCUNHO` ou `PENDENTE_DOCUMENTOS` → 409 `STATUS_NAO_EDITAVEL`;
  - **RN11**: `tipoDocumento=OUTRO` exige `descricaoPersonalizada` → 400;
  - multipart: salvar em `sgba.storage.dir`, registrar `nomeOriginal`, `tamanhoBytes`, `mimeType`, `caminhoArquivo` (limite 5MB, `application/pdf` — RF09 básico);
  - download como stream (`application/pdf`, `Content-Disposition` com nome original);
  - **RF15** validação do anexo pelo gerenciador: `PATCH` de `statusValidacao` (enum) + `observacaoValidacao`.
- [ ] `service/NotificacaoService.java` + **conectar o hook** da Issue 5: transições de status criam `Notificacao(destinatario=aluno, submissaoId, mensagem com protocolo/motivo)`.
- [ ] Controllers `DocumentoAnexoController`, `NotificacaoController`.
- [ ] **Evidência final (C5), coordenando as pastas das Issues 2–5:**
  - consolidar `sgba-checkpoint3.postman_collection.json` com TODAS as pastas/requests preenchidos (exemplos de bodies dos §6.2–§6.6);
  - adicionar testes/respostas de erro esperadas em cada request relevante;
  - `docs/demo-checkpoint3.md`: roteiro de demonstração ao vivo (ordem dos CLI, quais regras mostrar, dados do seed); atualizar `README.md`;
  - commit final da coleção + sanity: rodar coleção inteira do zero contra app re-subida (banco limpo + seed).

**Contrato REST**

| Verbo | Rota | Nota | Status |
|---|---|---|---|
| POST | `/api/submissoes/{id}/documentos` | multipart: `arquivo` + `tipoDocumento` (+ `descricaoPersonalizada` se OUTRO — RN11) só em RASCUNHO/PENDENTE (RN03) | **201** / 404 / **409** / 400 |
| GET | `/api/submissoes/{id}/documentos` | metadados (RF15) | 200 / 404 |
| GET | `/api/documentos/{id}/download` | stream do arquivo | 200 / **404** |
| DELETE | `/api/documentos/{id}` | só se submissão editável (RN03) | **204** / 404 / **409** |
| PATCH | `/api/documentos/{id}/status` | validação do gerenciador (RF15): `{ "statusValidacao": "INCOMPLETO", "observacaoValidacao": "PDF ilegível" }` — **requer função GERENCIADOR** (§9.2) | 200 / 400 (enum/observação/função) / 404 |
| GET | `/api/notificacoes?somenteNaoLidas=true` | inbox (RF17), header/id do destinatário (D1) | 200 |
| PATCH | `/api/notificacoes/{id}/lida` | marca lida → responde com NotificacaoResponse atualizada (alternativa aceita: 204 — decidir e documentar no PR; escolha padrão: 200 + body) | 200 / 404 / **409** se já lida? (padrão: idempotente 200) |

**Regras de negócio cobertas:** RN03 (documentos), RN11, RF09/RF15/RF17, RNF06.

**Critérios de aceite (Postman — pastas "Documentos" e "Notificações")**
- Upload de PDF → 201; download retorna os mesmos bytes enviados (conferir tamanho/tipo no Postman); tipo não-PDF → 400; > 5MB → 400; OUTRO sem descrição → 400; PATCH de validação visível na listagem de metadados (RF15).
- Notificação criada após mudança de status da Issue 5; `somenteNaoLidas=true` filtra; marcar como lida idempotente.
- **Coleção final rodar completa/integra (0 falhas) contra app nova com seed** — este é o *deliverable* do C5.
- Roteiro `demo-checkpoint3.md` executável em < 10 min cobrindo os 5 critérios.

**Fora do escopo:** antivírus/precisão MIME avançada (cp4), notificações por e-mail, `@PreAuthorize` (cp4), PDF/CSV (futuro).

---

## 7. Mapa de cobertura dos critérios (verificação final do checkpoint)

| Critério | Evidência esperada | Issue(s) |
|---|---|---|
| C1 Entidades ↔ DER | Tabela comparativa + divergências corrigidas (`Submissao.programa` removida; 5 uniques; not-null) + DBML v3 justificado no frontend | I1 |
| C2 Repositories core | 10 interfaces `*Repository` com consultas de negócio (não só básicas) | I1 (infra), I2–I6 |
| C3 Service com regra real | RNs implementadas e demonstráveis: RN01–RN05, RN07–RN12 (D1 substitui demanda de auth) | I2–I6 |
| C4 Controllers coerentes | Tabela de contrato por módulo; 201+Location, 204, 404/409/400 semânticos via handler base | I1 (handler), I2–I6 |
| C5 Evidência | Coleção Postman final 100% passando + `demo-checkpoint3.md` + seed | I1 (esqueleto) → I6 (final) |

## 8. Riscos e mitigação

| Risco | Mitigação |
|---|---|
| Audit da Issue 1 revelar mais divergências entidades×DER | A tabela comparativa vem PRIMEIRO (dia 1) — atraso máximo de 1 dia nos módulos; contrato por seção §6 evita reescrever DTOs |
| Issue 5 esperar módulos 2–4 | Services/repos/DTOs podem ser escritos contra o seed (Issue 1 fixa IDs de exemplo); integração é o último passo |
| Conflito de merge no `GlobalExceptionHandler`/seed | Issue 1 é a única dona desses arquivos; Issues 2–6 só **consomem** as exceções de serviço |
| Retrabalho por endpoints serem mudados no cp4 (auth) | D1 (header) + assinaturas de service isolam mudança; controllers serão ajustados no cp4 sem tocar nas RNs |
| multipart/`storage` falhar na demo | seed + armazenamento testado na Issue 1 (rota de prova opcional); Issue 6 tem rota de download sobressalente com arquivos pequenos |

## 9. Decisões do parceiro (aprovadas em 02/11)

1. **Política de conformidade com o DER (C1):** ✅ **DBML v3 com justificativa.** A Issue 1 inclui: (a) entidades ajustadas aos fluxos (`submissoes.protocolo` e `editais.caminho_arquivo_pdf` nullable/optional — campos "preenchidos no envio/upload"); (b) **atualização do `der.dbml` para v3** no repo frontend (`sgba-frontend/docs/der.dbml`) com comentários de justificativa por mudança (protocolo not-null efetivo no envio; caminho_arquivo_pdf preenchido no upload), replicando o padrão v2 ("troca de programa_id"); (c) log em `der-mudancas.md`; (d) tabela v2→v3 no PR. Assim entidades ↔ DER continuam 1:1 na arguição.
2. **Checagem leve de role no cp3:** ✅ **Somente nos 2 endpoints críticos** — `PATCH /api/submissoes/{id}/status` (Issue 5) e `PATCH /api/documentos/{id}/status` (Issue 6): o Service exige `funcao=GERENCIADOR` (400 `PERMISSAO_INSUFICIENTE`). Demais endpoints ficam abertos, documentados para o cp4. **RN06** passa a aceitar consulta ao detalhe/timeline/documentos do recurso com owner OR função `GERENCIADOR` (necessário para RF15).

## 10. Próximos passos após aprovação

1. Criar as 6 issues no GitHub (`gh`) com o corpo de cada seção §6 (títulos + labels + assignees conforme distribuição §5).
2. Chat/quadro de projeto: colunas `Backlog → Em progresso → Review → Done do checkpoint`.
3. Após o checkpoint, as issues de cp4 (validação/exceções/auth) herdam D1/D2 sem quebrar os módulos.
