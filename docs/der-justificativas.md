# Justificativas de Modelagem — DER SGBA (Checkpoint 2)

Fonte: `docs/der.dbml` + `requisitos.md`

---

## 1. Decisões e suas justificativas

### 1.1 Tabela única `usuarios` com enum `funcao` (opção 1)

**Alternativas descartadas:** (a) duas tabelas `alunos` e `gerenciadores`; (b) herança JPA com `PERFIL_ALUNO` 1:1.

**Justificativa:** ambos os perfis compartilham a mesma identidade de autenticação (email, senha, nome) e só divergem em atributos opcionais. Duas tabelas forçariam duplicar a lógica de login (união de tabelas) e impossibilitariam uma FK única em `historicos_status.usuario_id` — o autor de um evento pode ser aluno *ou* gerenciador. A tabela única com discriminator mantém **integridade referencial simples** e espelha diretamente o `ROLE_ALUNO`/`ROLE_GERENCIADOR` do Spring Security (RNF03). Custo: colunas acadêmicas nulas para gerenciadores — aceitável, pois são poucas e estáveis.

### 1.2 Dados acadêmicos nullable na mesma tabela (cpf, matrícula, cra...)

**Justificativa:** esses atributos **dependem da chave primária** (dependem do usuário), não de outro atributo — portanto não violam a 2FN/3FN. Normalização resolve dependência entre atributos, não campos "que só se aplicam a um subconjunto das linhas"; para isso, o mecanismo correto seria especialização (herança), que rejeitamos por custo/benefício (ver 1.1). A regra "gerenciador não tem matrícula" é **regra de negócio validada na aplicação**, não estrutura.

### 1.3 `status` atual em `submissoes` **e** `historicos_status` separado

**Por que não é redundância:** a coluna `submissoes.status` guarda o **estado corrente** (fato atual, usado em toda query de listagem — RF11, RF14); cada linha de `historicos_status` é um **evento distinto** com autor, timestamp e observação (fato histórico). São duas projeções diferentes do mesmo ciclo de vida. Eliminar o histórico quebraria RN07 (auditoria imutável); eliminar o status atual obrigaria derivar o estado com subquery/ORDER BY em toda listagem — custo alto, ganho zero.

**Imutabilidade (append-only):** a tabela é um log — só INSERT e SELECT. Isso garante trilha de auditoria confiável (RN07) e permite reconstituir a timeline do aluno (RF12) sem risco de reescrita.

### 1.4 Enums no lugar de strings livres / tabelas lookup

**Justificativa:** os domínios (`status_submissao`, `tipo_documento`, etc.) são **fechados e definidos no DRS**. Enum garante que nenhum valor inválido entre no banco (integridade de domínio), elimina strings mágicas e documenta o modelo. Tabelas lookup seriam mais genéricas, mas adicionam JOINs e CRUD administrativo desnecessários para um domínio que só muda via migração — YAGNI.

### 1.5 Metadados de arquivo em vez do blob (BLOB/CLOB)

**Justificativa:** `caminho_arquivo`, `tamanho_bytes`, `mime_type` descrevem o arquivo; o arquivo em si vive em disco (RNF06). Guardar blobs no banco: (a) inflaria backups e buffer pool, (b) degradaria queries de listagem (`SELECT *` arrastaria MBs), (c) dificultaria streaming para download. Metadados + caminho é o padrão de mercado (S3 + metadados no RDBMS). `tamanho_bytes` e `mime_type` não são redundância: são **verificações de integridade** lidas no download, não deriváveis do caminho.

### 1.6 Chaves únicas compostas

- `(numero_edital, ano)` em `editais`: editais são identificados pelo par número+ano (ex.: Edital 01/2025), não por um id técnico isolado.
- `(aluno_id, programa_id)` em `submissoes`: impede que o mesmo aluno inscreva-se duas vezes no mesmo programa — **constraint estrutural mais forte que validação na aplicação**, pois sobrevive a concorrência e a bugs de código.

### 1.7 `protocolo` como varchar UK (#AAAA-XXXX) e não inteiro sequencial

**Justificativa:** o protocolo é um **identificador de negócio** com formato definido (RN02), exposto ao usuário e impresso no PDF. Separá-lo do id técnico permite mudar o formato futuro sem migrar todas as FKs. UK garante unicidade.

### 1.8 `historicos_status.usuario_id` apontando para `usuarios` (e não só gerenciador)

**Justificativa:** o próprio aluno gera eventos ("Enviada pelo aluno", RF12). Uma FK única para `usuarios` cobre os dois atores sem tabela de lookup de papéis.

### 1.9 `observacao_validacao` em `documentos_anexo`

**Justificativa:** RF13 exige que a comissão aponte *qual documento* tem problema. Sem essa coluna, a pendência ficaria só no nível da submissão e o aluno não saberia qual arquivo reenviar.

---

## 2. A modelagem é genérica o suficiente?

**Resposta curta: é genérica o suficiente para o escopo do DRS/telas do Figma, e deliberadamente NÃO genérica além disso.**

Avaliação honesta — onde a genericidade foi sacrificada e o que custaria generalizar:

| Ponto | Limitação atual | Generalização possível | Vale a pena aqui? |
|---|---|---|---|
| Papéis de usuário | Enum fixo (2 papéis) | Tabela `papeis` + N:N | Não — DRS garante 2 papéis; enum é mais seguro |
| Tipos de documento | Enum global fixo | Programa define seus documentos exigidos (N:N `programas_exigencias`) | Seria a **evolução natural** se programas pudessem exigir docs personalizados; DRS fixa 3 tipos |
| Submissão → programa | Uma submissão = 1 programa | N:N com tabela de associação | Não — cada edital tem formulário próprio (RF07) |
| CRA | Declarado pelo aluno | Tabela `avaliacoes_academicas` por período | Não — fora do escopo; CRA validado manualmente pela comissão |
| `status_programa` | Coluna manual | Derivável de `data_limite`/datas de publicação | Redundância leve, **consciente**: status pode ser "EM_BREVE" antes da data; manter explícito dá controle ao gerenciador |
| Log de auditoria | Específico para status | Tabela genérica de eventos | Não — tipar os eventos em enum dá consultas e FKs mais ricas |

**Conclusão para a defesa:** o modelo atende a 3FN, cobre todos os RFs e RNs com constraints estruturais (UKs, FKs, enums, NOT NULL) — não apenas validações de aplicação — e as poucas genericidades abertas (ex.: documentos exigidos por programa) estão **identificadas e têm caminho de evolução claro**, o que é exatamente o que se espera de um modelo conceitual bem feito: não antecipar requisitos que não existem (YAGNI), mas não fechar portas.



PRoblema com o CRA desatualizado
Cadastrar função de professor/orientador com professor id ou orientador id

Relações de programa bolsa e submissão. Talvez seja melhor relacionar diretamente submissões ao edital.

Mais tipos de documento_anexo para garantir a contemplação de mais tipos de documentos + coluna de outras; Se for do tipo Outros pedir para determinar que tipo é esse.

Tabela de Notificação para guardar notificações 

Possivel nova tabela de formulário para ter mais genérico possivel no caso de criar um novo edital.

Deixar o sistema mais genérico que seja possível determinar o que cada edital e novos editais podem pedir para uma submissão futura.
