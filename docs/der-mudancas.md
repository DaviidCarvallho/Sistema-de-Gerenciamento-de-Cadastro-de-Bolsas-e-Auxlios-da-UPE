# SGBA — DER v1 → v2: Registro de Mudanças

**Disciplina:** Programação Web — UPE / PROPG
**Arquivo atual:** `docs/der.dbml`
**Modelo anterior:** Checkpoint 2 (tabela única `usuarios`, `submissoes.programa_id`)
**Modelo novo:** v2 pós-feedback Prof. Murilo (Grupo 1)
**Como visualizar:** colar `docs/der.dbml` em dbdiagram.io

## Resumo executivo

| # | Comentário do professor | Mudança aplicada | Onde |
|---|---|---|---|
| 1 | Falta `data_abertura` em programa | `programas_bolsa.data_abertura` + prazo por edição em `editais` | `docs/der.dbml:107`, `docs/der.dbml:120-121` |
| 2 | Dados acadêmicos misturados + CRA/período voláteis | Nova tabela `alunos` 1:1 + snapshot em `submissoes` | `docs/der.dbml:73-97`, `docs/der.dbml:159-162` |
| 3 | Faltam papéis: Professor | `funcao_usuario` + `PROFESSOR` | `docs/der.dbml:10-14` |
| 4 | Orientador como `varchar` | `submissoes.orientador_id FK` | `docs/der.dbml:157`, `docs/der.dbml:231` |
| 5 | `status` + `historicos_status` = redundância / falha | Contrato: cache + fonte da verdade, escrita transacional | `docs/der.dbml:149`, `docs/der.dbml:166`, `docs/der.dbml:201-211` |
| 6 | Submissão ligada a programa, não a edital | `submissoes.edital_id FK`, removido `programa_id` | `docs/der.dbml:156`, `docs/der.dbml:230` |
| 7 | `UNIQUE` por programa impede reedições | `UNIQUE(aluno_id, edital_id)` | `docs/der.dbml:168-170` |
| 8 | Falta formulário completo | Campos pessoais/endereço em `alunos`, proposta em `submissoes`, `Enum genero` | `docs/der.dbml:16-21`, `docs/der.dbml:73-97`, `docs/der.dbml:150-152` |
| 9 | `tipo_documento` incompleto | `+ PLANO_TRABALHO, OUTRO` + `descricao_personalizada` | `docs/der.dbml:46-52`, `docs/der.dbml:189` |
| 10 | Sem aviso de pendência | Nova `notificacoes` | `docs/der.dbml:213-222` |
| 11 | Perguntas por edital + respostas + o que cada edital exige (6/7) | `perguntas_edital` + `respostas_submissao` + `exigencias_edital` | `docs/der.dbml:132-159`, `docs/der.dbml:188-199` |

---

## 1. `data_abertura` + prazo por edição

**Antes:**
```dbml
Table programas_bolsa {
  data_limite timestamp [not null]
}
Table editais {
  data_publicacao date [not null]
}
```

**Depois (`docs/der.dbml:107`, `docs/der.dbml:120-121`):**
```dbml
Table programas_bolsa {
  data_abertura timestamp [not null]
  data_limite timestamp [not null]
}
Table editais {
  data_inicio_submissoes timestamp [not null]
  data_fim_submissoes timestamp [not null]
}
```

**Motivo:** só com `data_limite` não é possível representar `EM_BREVE`, nem validar janela de inscrição. Prazo real varia por edição, não por programa genérico (RN05).

**Benefício:** permite consulta `ABERTO = now() BETWEEN abertura/limite`, valida `data_envio` contra `editais.data_fim_submissoes`, e mantém `programas_bolsa.status` como controle manual + datas como regra dura.

## 2. Separar `alunos` + snapshot de CRA/período

**Antes (`usuarios` monolítica):**
```dbml
Table usuarios {
  funcao funcao_usuario [not null]
  cpf char(11) [null, unique]
  matricula varchar(20) [null, unique]
  curso varchar(100) [null]
  periodo smallint [null]
  cra decimal(4,2) [null]
}
```

**Depois (`docs/der.dbml:63-97`, `docs/der.dbml:159-162`):**
```dbml
Table usuarios {
  funcao funcao_usuario [not null]
  // sem colunas acadêmicas
}
Table alunos {
  usuario_id bigint [pk] // PK = FK
  periodo_atual smallint [null]
  cra_atual decimal(4,2) [null]
  // + rg, nacionalidade, genero, telefone, endereco...
}
Table submissoes {
  curso_momento varchar(100) [null]
  periodo_momento smallint [null]
  cra_momento decimal(4,2) [null]
  unidade_momento varchar(50) [null]
}
Ref: alunos.usuario_id - usuarios.id
Ref: submissoes.aluno_id > alunos.usuario_id
```

**Motivo:**
1. Gerenciador/professor ficavam com 7+ colunas sempre nulas — especialização mal modelada.
2. `cra` e `periodo` mudam todo semestre. Se o aluno atualiza o perfil, a comissão perde o valor que fundamentou o deferimento.

**Benefício:** sem nulos espúrios; FK `submissoes.aluno_id > alunos.usuario_id` garante em nível estrutural que só ALUNO submete; snapshot congela mérito no `data_envio` para auditoria e geração do PDF (RF10/RF15). Regra de cópia: `Service` copia `cra_atual → cra_momento` na transição `RASCUNHO → RECEBIDA`.

## 3. Função `PROFESSOR`

**Antes:**
```dbml
Enum funcao_usuario { ALUNO GERENCIADOR }
```

**Depois (`docs/der.dbml:10-14`):**
```dbml
Enum funcao_usuario { ALUNO PROFESSOR GERENCIADOR }
```

**Motivo:** orientador é ator do domínio (RF07 etapa 2). Sem papel próprio não há como referenciá-lo com integridade.

**Benefício:** habilita RBAC `ROLE_PROFESSOR`, FK de orientação, futuras telas do professor sem gambiarra.

## 4. Orientador como FK, não `varchar`

**Antes:**
```dbml
Table submissoes {
  orientador_nome varchar(150) [null]
  orientador_email varchar(255) [null]
}
```

**Depois (`docs/der.dbml:157`):**
```dbml
Table submissoes {
  orientador_id bigint [null] // FK -> usuarios.id (funcao=PROFESSOR)
}
```

**Motivo:** `varchar` livre gera `José Silva` vs `J. Silva vs jose.silva@upe.br`, duplicatas, sem validação de existência, sem como notificar ou listar "minhas orientações".

**Benefício:** integridade referencial, busca indexada, base para notificar o professor e validar `orientador_id` na aplicação (`funcao == PROFESSOR`).

## 5. `status` atual vs `historicos_status` — redundância controlada

**Antes:** duas representações sem contrato — risco de `submissoes.status = APROVADA` divergir do último evento do histórico.

**Depois (`docs/der.dbml:149`, `docs/der.dbml:166`, `docs/der.dbml:210`):**
```dbml
Table submissoes {
  status status_submissao [not null, default: 'RASCUNHO'] // cache
}
Table historicos_status {
  // append-only, nunca UPDATE/DELETE
}
```

**Motivo:** não são o mesmo fato. `submissoes.status` = estado corrente (usado em todas as listagens RF11/RF14). `historicos_status` = eventos com autor/hora/motivo (RN07/RF12). Remover um dos dois quebra ou performance ou auditoria.

**Benefício / regra anti-falha:** fonte da verdade histórica = `historicos_status`; `status` = cache. Escrita obrigatória em transação: `INSERT historicos_status + UPDATE submissoes.status`. Leitura de lista usa `status` (O(1)); timeline/auditoria usa histórico. Alternativa descartada (derivar status com `ORDER BY data_hora DESC LIMIT 1` em toda query) foi rejeitada por custo.

## 6. Submissão ligada ao `edital`, não ao `programa`

**Antes:**
```dbml
Table submissoes { programa_id bigint [not null] }
Ref: submissoes.programa_id > programas_bolsa.id
```

**Depois (`docs/der.dbml:156`, `docs/der.dbml:230`):**
```dbml
Table submissoes { edital_id bigint [not null] }
Ref: submissoes.edital_id > editais.id
// programa via JOIN: submissoes -> editais -> programas_bolsa
```

**Motivo:** candidatura concorre a uma edição (número/ano, prazo, vagas, perguntas específicas), não ao programa abstrato. Manter `programa_id` duplicaria caminho (`submissao→programa` vs `submissao→edital→programa`) — segunda redundância.

**Benefício:** prazo, documentos e perguntas passam a ser por edição; elimina ambiguidade; simplifica RN05 e filtros por edital.

## 7. `UNIQUE` por edição

**Antes:**
```dbml
indexes { (aluno_id, programa_id) [unique] }
```

**Depois (`docs/der.dbml:168-170`):**
```dbml
indexes { (aluno_id, edital_id) [unique, name: 'uk_aluno_edital'] }
```

**Motivo:** `UNIQUE` por programa impedia o aluno de tentar o mesmo programa no semestre seguinte — restritivo demais. Sem `UNIQUE` por edição, nada impedia duplo `POST` concorrente criar 2 rascunhos/enviadas.

**Benefício:** permite recandidatura em edições futuras, bloqueia duplicata na mesma edição em nível de banco (sobrevive a race condition, mais forte que validação em `Service`).

## 8. Campos do formulário

**Antes:** sem `nacionalidade, genero, RG, telefone, endereco, semestre_ingresso, titulo_projeto, plano_trabalho`.

**Depois (`docs/der.dbml:16-21`, `docs/der.dbml:73-97`, `docs/der.dbml:150-152`):**
```dbml
Enum genero { FEMININO MASCULINO OUTRO PREFIRO_NAO_INFORMAR }
Table alunos {
  rg varchar(20) [null]
  nacionalidade varchar(50) [null]
  genero genero [null]
  telefone varchar(20) [null]
  semestre_ingresso varchar(10) [null]
  logradouro varchar(150) [null]
  numero varchar(20) [null]
  complemento varchar(100) [null]
  bairro varchar(100) [null]
  cidade varchar(100) [null]
  uf char(2) [null]
  cep char(8) [null]
}
Table submissoes {
  titulo_projeto varchar(200) [null]
  projeto_resumo text [null]
  plano_trabalho text [null]
}
```

**Motivo:** RF07/RF10/RF18 exigem PDF padronizado e CSV com esses dados. Endereço embutido em `alunos` é suficiente para Checkpoint 2; extrair para tabela `enderecos` só se precisar histórico multi-endereço.

**Benefício:** separação clara: dado estável do aluno fica em `alunos`; dado da proposta fica em `submissoes` (permite títulos diferentes por edital).

## 9. `tipo_documento` + `OUTRO`

**Antes:**
```dbml
Enum tipo_documento { HISTORICO_ESCOLAR COMPROVANTE_MATRICULA CARTA_INTENCAO }
```

**Depois (`docs/der.dbml:46-52`, `docs/der.dbml:189`):**
```dbml
Enum tipo_documento {
  HISTORICO_ESCOLAR COMPROVANTE_MATRICULA CARTA_INTENCAO
  PLANO_TRABALHO OUTRO
}
Table documentos_anexo {
  descricao_personalizada varchar(255) [null]
}
```

**Motivo:** editais pedem plano de trabalho e documentos ad-hoc. Enum fechado de 3 valores forçaria misuse (ex.: salvar plano como `CARTA_INTENCAO`).

**Benefício:** consultas continuam tipadas por enum; `OUTRO + descricao_personalizada` (validar `NOT NULL` na aplicação quando `tipo == OUTRO`) dá extensibilidade sem migração.

## 10. Tabela `notificacoes`

**Antes:** só `historicos_status`; aluno descobria pendência por polling ou e-mail externo.

**Depois (`docs/der.dbml:213-222`, `docs/der.dbml:237-238`):**
```dbml
Table notificacoes {
  destinatario_id bigint [not null]
  submissao_id bigint [null]
  mensagem varchar(1000) [not null]
  lida_em timestamp [null]
  criado_em timestamp [not null, default: `now()`]
}
```

**Motivo:** RF13 (`PENDENTE_DOCUMENTOS → Resolver pendência`) e RF17 (feedback) precisam de inbox consultável.

**Benefício:** `WHERE destinatario_id = ? AND lida_em IS NULL` alimenta badge/sino; `submissao_id` dá contexto para deep-link; desacopla de e-mail.

## 11. Formulário dinâmico: `perguntas_edital` + `respostas_submissao` + `exigencias_edital` (itens 6/7 — aplicado)

**Antes:** formulário hardcoded em colunas de `submissoes`. Todo edital novo = `ALTER TABLE` + deploy. Documentos exigidos eram globais, sem vínculo por edição.

**Depois (`docs/der.dbml:132-159`, `docs/der.dbml:188-199`):**
```dbml
Table perguntas_edital {
  edital_id bigint [not null]
  enunciado text [not null]
  obrigatoria boolean [not null, default: true]
  ordem_exibicao smallint [not null]
  indexes { (edital_id, ordem_exibicao) [unique] }
}
Table respostas_submissao {
  submissao_id bigint [not null]
  pergunta_id bigint [not null]
  resposta_texto text [not null]
  indexes { (submissao_id, pergunta_id) [unique] }
}
Table exigencias_edital {
  edital_id bigint [not null]
  tipo_documento tipo_documento [not null]
  obrigatorio boolean [not null, default: true]
  indexes { (edital_id, tipo_documento) [unique] }
}
```

**Motivo:** cada edital pede perguntas e documentos diferentes (itens 6 e 7). Sem isso, o modelo trava na primeira edição.

**Benefício:** gerenciador cadastra por edição (RF04) o que será pedido; envio valida `obrigatoria`/`obrigatorio` na aplicação; `UNIQUE(submissao, pergunta)` e `UNIQUE(edital, tipo_documento)` impedem duplicada. Campos universais (`titulo_projeto`, `resumo`) continuam colunas fixas por serem indexáveis; o variável vai para EAV controlado. Decisão: vale aplicar — 3 tabelas pequenas, sem renderer dinâmico complexo no Checkpoint 2.

---

## Regras de escrita que evitam as falhas apontadas

1. Envio de submissão (transação): copiar `alunos.cra_atual/periodo/curso → submissoes.*_momento`, setar `data_envio`, `INSERT historicos_status(RECEBIDA)`, `UPDATE submissoes.status`.
2. Decisão do gerenciador (transação): `INSERT historicos_status + UPDATE submissoes.status + INSERT notificacoes`.
3. `historicos_status`: só `INSERT/SELECT`. Sem `UPDATE/DELETE` (convenção + `GRANT`/`@PreAuthorize`).
4. `tipo_documento == OUTRO` exige `descricao_personalizada` — Bean Validation.
5. RN04: `PENDENTE_DOCUMENTOS/INDEFERIDA` exigem `historicos_status.observacao` — Bean Validation + `CHECK` futuro.
6. RN05: validar `now() BETWEEN editais.data_inicio/fim_submissoes`, não só `programas_bolsa.data_limite`.
7. Envio valida `perguntas_edital.obrigatoria x respostas_submissao` e `exigencias_edital.obrigatorio x documentos_anexo`.

---

# SGBA — DER v2 → v3: Registro de Mudanças (Checkpoint 3 — Issue 1)

**Arquivo atual:** `docs/der.dbml`
**Modelo anterior:** v2 (pós-feedback Prof. Murilo — seções acima)
**Modelo novo:** v3 — "fidelização": alinhando o DER às entidades JPA da implementação do Checkpoint 3 (entities ↔ DER 1:1 para a arguição, critério C1).
**Como visualizar:** colar `docs/der.dbml` em dbdiagram.io

## Resumo executivo v2 → v3

| # | Mudança | Antes (v2) | Depois (v3) | Justificativa | Onde |
|---|---|---|---|---|---|
| 1 | `submissoes.protocolo` vira nullable | `protocolo varchar(10) [not null, unique]` | `varchar(10) [null, unique]` | Fluxo DRS: submissão nasce em `RASCUNHO` **sem protocolo**; o protocolo é gerado somente no `enviar` (RN02 — formato `#AAAA-XXXX` com contador anual, decisão D6). Not-null efetivo acontece na transição `RASCUNHO → RECEBIDA`: a coluna é preenchida 100% das submissões enviadas, mas informar `not null` impediria criar o rascunho (RF07 passo 1). A UK permanece para garantir unicidade após o envio. | `docs/der.dbml` §submissoes |
| 2 | `editais.caminho_arquivo_pdf` vira nullable | `caminho_arquivo_pdf varchar(500) [not null]` | `varchar(500) [null]` | Fluxo DRS: o gerenciador cria o edital primeiro por JSON (RF04) e anexa o PDF oficial na sequência (upload, RNF06). Exigir `not null` na criação impediria o CRUD sem arquivo pré-existente. Preenchimento efetivo no upload. | `docs/der.dbml` §editais |
| 3 | `usuarios` ganha `criado_em` / `atualizado_em` | (ausentes) | `timestamp [not null, default: now()]` / `[null]` | Entidades do cp3 já registram os rastros de auditoria; o DER passa a documentá-los para manter entidades ↔ DER 1:1 (C1). Demais tabelas (`programas_bolsa`, `submissoes`, `notificacoes`...) já tinham `criado_em` na v2. | `docs/der.dbml` §usuarios |

**Intencionalmente mantido da v2:** as 5 uniques (`uk_edital_numero_ano`, `uk_pergunta_ordem`, `uk_exigencia_edital_tipo`, `uk_aluno_edital`, `uk_resposta_unica`) — as entidades JPA do backend as implementaram fielmente (`@UniqueConstraint` em `Edital`, `PerguntaEdital`, `ExigenciaEdital`, `Submissao`, `RespostaSubmissao`); `submissoes.edital_id` continua no lugar de `programa_id` (programa é obtido via `submissao.edital.programa`).

## Nota para o Checkpoint 4 (futuro)

`historicos_status` e `documentos_anexo` seguem sem mudança estrutural; completa-campo comparativo entidades × DER v3 está em `sgba-backend/docs/auditoria-entidades-der.md` (PR da Issue 1).
