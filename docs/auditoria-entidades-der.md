# Auditoria de Entidades × DER v3 (C1) — Checkpoint 3

**Fonte normativa:** `sgba-frontend/docs/der.dbml` **v3** (mudanças v2→v3 em
`sgba-frontend/docs/der-mudancas.md`). Cada divergência encontrada pela auditoria campo-a-campo
foi **corrigida na entidade** (entidades nunca ficam à frente do DER — decisão §9.1).

## Veredicto por entidade (11 entidades + 7 enums)

| Entidade | Tabela | Campos | Unique Constraints | Status pós-auditoria |
|---|---|---|---|---|
| `Usuario` | `usuarios` | id, nome, email, senha_hash, funcao, criado_em, atualizado_em | `email` | ✅ conforme (DER v3 ganhou criado/atualizado p/ casar) |
| `Aluno` | `alunos` | usuario_id (MapsId), cpf, rg, data_nascimento, nacionalidade, genero, telefone, matricula, curso, periodo_atual, cra_atual, unidade_academica, semestre_ingresso + 9 campos de endereço | `cpf`, `matricula` | ✅ conforme (`curso`/`matricula` not null confirmados) |
| `ProgramaBolsa` | `programas_bolsa` | id, titulo, categoria, descricao, valor_mensal, vagas, periodo_letivo, data_abertura, data_limite, status, criado_em | — | ✅ conforme (nullable corrigidos, ver §divergências) |
| `Edital` | `editais` | id, numero_edital, ano, data_publicacao, data_inicio_submissoes, data_fim_submissoes, caminho_arquivo_pdf, programa_id | `uk_edital_numero_ano` | ✅ conforme (`caminho_arquivo_pdf` nullable — v3) |
| `PerguntaEdital` | `perguntas_edital` | id, edital_id, enunciado, obrigatoria, ordem_exibicao | `uk_pergunta_ordem` | ✅ conforme |
| `ExigenciaEdital` | `exigencias_edital` | id, edital_id, tipo_documento, obrigatorio | `uk_exigencia_edital_tipo` | ✅ conforme |
| `Submissao` | `submissoes` | id, protocolo, status, titulo_projeto, projeto_resumo, plano_trabalho, termo_aceite, data_envio, aluno_id, edital_id, orientador_id, snapshot (`*_momento`), criado_em, atualizado_em | `uk_aluno_edital`, `protocolo` | ✅ conforme — **sem FK `programa`** (via `edital.programa`), `protocolo` nullable (v3) |
| `RespostaSubmissao` | `respostas_submissao` | id, submissao_id, pergunta_id, resposta_texto | `uk_resposta_unica` | ✅ conforme (`resposta_texto` not null corrigido) |
| `DocumentoAnexo` | `documentos_anexo` | id, tipo_documento, descricao_personalizada, nome_original, caminho_arquivo, tamanho_bytes, mime_type, status_validacao, observacao_validacao, submissao_id | — | ✅ conforme (`tamanho_bytes`/`mime_type` not null corrigidos) |
| `HistoricoStatus` | `historicos_status` | id, status_anterior, status_novo, observacao, data_hora, submissao_id, usuario_id | — | ✅ conforme (`status_anterior` nullable — primeira inserção) |
| `Notificacao` | `notificacoes` | id, destinatario_id, submissao_id, mensagem, lida_em, criado_em | — | ✅ conforme |
| **Enums (7)** | — | funcao_usuario, genero, categoria_programa, status_programa, status_submissao, tipo_documento, status_validacao_documento | — | ✅ 7/7 com valores idênticos às `Enum` do DBML |

## Divergências corrigidas nesta issue (além do §2.4 do design)

O §2.4 do design já previa: remoção de `Submissao.programa`, `protocolo` nullable,
`Edital.caminhoArquivoPdf` nullable, 5 uniques e `Aluno.matricula` not null — todas já presentes.
A varredura campo-a-campo **revelou e corrigiu** os seguintes `nullable` divergentes do DBML v3
(cada um marcado `[not null]` no DER, mas ausentes como `nullable=false` na entidade):

| # | Entidade `. campo` | Entidades antes | Correção |
|---|---|---|---|
| 1 | `Aluno.curso` | sem `not null` | `@Column(length=100, nullable=false)` — DBML: `curso [not null]` |
| 2 | `ProgramaBolsa.valorMensal` | sem `not null` | `nullable=false` (decimal(10,2)) |
| 3 | `ProgramaBolsa.vagas` | sem `not null` | `nullable=false` (default 0 vira responsabilidade do service) |
| 4 | `ProgramaBolsa.periodoLetivo` | sem `not null` | `nullable=false` |
| 5 | `ProgramaBolsa.dataAbertura` / `dataLimite` | sem `not null` | `nullable=false` (RN05 depende delas) |
| 6 | `ProgramaBolsa.criadoEm` | sem `not null` e sem inicialização | `nullable=false` + `@PrePersist` (`now()`) |
| 7 | `Edital.dataPublicacao`, `dataInicioSubmissoes`, `dataFimSubmissoes` | sem `not null` | `nullable=false` (janela RN05 não pode ser nula) |
| 8 | `Submissao.termoAceite` | sem `not null` | `nullable=false` (DBML: default false na criação, exigido true no envio — RN12) |
| 9 | `Submissao.criadoEm`/`atualizadoEm` | **campos ausentes** (DBML: `[not null]`) | adicionados + `@PrePersist`/`@PreUpdate` |
| 10 | `RespostaSubmissao.respostaTexto` | sem `not null` | `nullable=false` |
| 11 | `DocumentoAnexo.tamanhoBytes` | sem `not null` | `nullable=false` (verificação no download) |
| 12 | `DocumentoAnexo.mimeType` | sem `not null` | `nullable=false` (default `application/pdf` no service — RN09/RF09) |

## Mapeamentos estruturais verificados

- `GenerationType.IDENTITY` em todas as FKs técnicas (decisão D5) — ✅ presente nas entidades.
  - `Aluno` usa `@MapsId` (PK = FK `usuario_id`) — ✅.
- Snapshot em `submissoes`: `curso_momento`, `periodo_momento`, `cra_momento`, `unidade_momento` (RN09) — ✅ campos presentes e nullable.
- `usuarios.nacionalidade` default `'Brasileira'` — **regra de aplicação** (service/DTO), sem constraint no banco; tratado como decisão documentada, não como divergência.
- Tipos: `varchar`/`char` do DBML viram `String` com `length` equivalente; `decimal` → `BigDecimal` com `precision/scale`, `smallint` → `Short`, `timestamp` → `LocalDateTime`, `date` → `LocalDate`; `text` → `String` + `columnDefinition="TEXT"`.

## Árvore de rastreabilidade

- `sgba-frontend/docs/der.dbml` (v3) — fonte;
- `sgba-frontend/docs/der-mudancas.md` (seção v2→v3) — mudanças 1–3 justificadas;
- entidades: `src/main/java/br/edu/upe/sgba/entity/**` — onde as correções acima foram aplicadas.
