# Plano — Completar Issue 1 (Fundação) do Checkpoint 3

Fonte: `docs/superpowers/specs/2026-11-02-checkpoint3-backend-issues-design.md` §6 Issue 1.
Diagnóstico (05/11): entidades core já ajustadas ao DER v3; faltam infra/erros/seed/evidência.
Erros do colega reproduzidos: (1) `mvnw` sem permissão de execução; (2) ausência de JDK 21
(`javac` → `release version 21 not supported`); (3) sem `compose.yaml`/datasource (app não sobe).

## Checklist

- [x] Ambiente: JDK 21 em `~/.jdks` (JRE 25 não compila) + `chmod +x mvnw` + README
- [x] `pom.xml`: dependência H2 (escopo test) para o profile `test`
- [x] `compose.yaml`: postgres:16, db/user/senha `sgba`, porta 5432, healthcheck, volume
- [x] `application.properties` (comum): `sgba.storage.dir` (D7); `application-dev.properties` (PG local,
      ddl-auto=update, show-sql=true); `application-test.properties` (H2 in-memory)
- [x] Auditoria campo-a-campo: registrar e corrigir divergências residuais
      (`Aluno.curso` not null; `ProgramaBolsa` valor_mensal/vagas/periodo_letivo/datas/criado_em;
      `Submissao.termoAceite` not null + criado_em/atualizado_em app-managed; `RespostaSubmissao.respostaTexto`
      not null; `DocumentoAnexo` tamanhoBytes/mimeType; `Edital.dataPublicacao/datas` not null)
- [x] `docs/auditoria-entidades-der.md`: tabela comparativa entidades×DER v3 (C1)
- [x] Erros base D2: `RecursoNaoEncontradoException`, `RegraNegocioException` (status HTTP, default 409),
      `ErrorResponse` (record), `GlobalExceptionHandler` (404/409/400 + rota inexistente)
- [x] `config/DevSeedData`: `@Profile("dev")`, `CommandLineRunner` — 3 usuários (aluno/professor/gerenciador),
      perfil Aluno completo, 1 ProgramaBolsa + Editais A (aberto, 2 perg. 1 obrig., 2 exig. opcionais),
      B (aberto, exig. obrigatórias — RN12), C (janela encerrada — RN05 409), D (infra/duplicatas)
- [x] DER v3 no `sgba-frontend/docs/der.dbml` + registro em `der-mudancas.md` (v2→v3) (commit d2e52fd no frontend)
- [x] Esqueleto evidência C5: `docs/postman/` com coleção vazia (`sgba-checkpoint3.postman_collection.json`,
      var `baseUrl`) + fragmentos por módulo; README (compose, rodar app, recarregar seed)
- [x] Documentar convenções D1–D4, D6–D9 + nomes de DTOs (`docs/decisoes-checkpoint3.md`)
- [x] Testes: `contextLoads` com `@ActiveProfiles("test")`, teste de handler (`/api/inexistente` → 404 com
      `ErrorResponse`) via `RestTestClient`
- [x] Verificação final: `./mvnw test` verde (6/6); `docker compose up -d` (healthy) + app profile dev +
      seed no PG (3 usuários, aluno, programa, editais A–D, 3 perguntas, 4 exigências; 5 uniques em
      pg_constraint); `./api/inexistente` → 404 ErrorResponse; idempotência do seed provada no restart

## Alterações pós-plano (ajustes de execução)

- JPQL de contagem do seed ganhando alias `x` (`SELECT COUNT(x) FROM Entidade x`) após SemanticException do Hibernate 7.4 (visto no primeiro boot com dev).
- IDs do README corrigidos: sequências IDENTITY são por tabela (usuários 1–3; programa 1; editais 1–4).
