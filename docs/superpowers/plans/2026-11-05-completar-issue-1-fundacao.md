# Plano — Completar Issue 1 (Fundação) do Checkpoint 3

Fonte: `docs/superpowers/specs/2026-11-02-checkpoint3-backend-issues-design.md` §6 Issue 1.
Diagnóstico (05/11): entidades core já ajustadas ao DER v3; faltam infra/erros/seed/evidência.
Erros do colega reproduzidos: (1) `mvnw` sem permissão de execução; (2) ausência de JDK 21
(`javac` → `release version 21 not supported`); (3) sem `compose.yaml`/datasource (app não sobe).

## Checklist

- [ ] Ambiente: JDK 21 em `~/.jdks` (JRE 25 não compila) + `chmod +x mvnw` + README
- [ ] `pom.xml`: dependência H2 (escopo test) para o profile `test`
- [ ] `compose.yaml`: postgres:16, db/user/senha `sgba`, porta 5432, healthcheck, volume
- [ ] `application.properties` (comum): `sgba.storage.dir` (D7); `application-dev.properties` (PG local,
      ddl-auto=update, show-sql=true); `application-test.properties` (H2 in-memory)
- [ ] Auditoria campo-a-campo: registrar e corrigir divergências residuais
      (`Aluno.curso` not null; `ProgramaBolsa` valor_mensal/vagas/periodo_letivo/datas/criado_em;
      `Submissao.termoAceite` not null + criado_em/atualizado_em app-managed; `RespostaSubmissao.respostaTexto`
      not null; `DocumentoAnexo` tamanhoBytes/mimeType; `Edital.dataPublicacao/datas` not null)
- [ ] `docs/auditoria-entidades-der.md`: tabela comparativa entidades×DER v3 (C1)
- [ ] Erros base D2: `RecursoNaoEncontradoException`, `RegraNegocioException` (status HTTP, default 409),
      `ErrorResponse` (record), `GlobalExceptionHandler` (404/409/400 + rota inexistente)
- [ ] `config/DevSeedData`: `@Profile("dev")`, `CommandLineRunner` — 3 usuários (aluno/professor/gerenciador),
      perfil Aluno completo, 1 ProgramaBolsa + Editais A (aberto, 2 perg. 1 obrig., 2 exig. opcionais),
      B (aberto, exig. obrigatórias — RN12), C (janela encerrada — RN05 409), D (infra/duplicatas)
- [ ] DER v3 no `sgba-frontend/docs/der.dbml` + registro em `der-mudancas.md` (v2→v3)
- [ ] Esqueleto evidência C5: `docs/postman/` com coleção vazia (`sgba-checkpoint3.postman_collection.json`,
      var `baseUrl`) + fragmentos por módulo; README (compose, rodar app, recarregar seed)
- [ ] Documentar convenções D1–D4, D6–D9 + nomes de DTOs (`docs/decisoes-checkpoint3.md`)
- [ ] Testes: `contextLoads` com `@ActiveProfiles("test")`, teste de handler (`/api/inexistente` → 404 com
      `ErrorResponse`) via `TestRestTemplate`
- [ ] Verificação final: `./mvnw test` verde; `docker compose up -d` + app com profile dev + seed no PG;
      `/api/inexistente` → 404 via curl
