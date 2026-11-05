# SGBA — Sistema de Gerenciamento de Bolsas e Auxílios da UPE

O **SGBA (Sistema de Gerenciamento de Bolsas e Auxílios da Universidade de Pernambuco)** é um projeto desenvolvido com o objetivo de centralizar e organizar o processo de gerenciamento de inscrições em programas de bolsas e auxílios oferecidos pela UPE.

A proposta é disponibilizar um ambiente digital no qual estudantes possam consultar editais, realizar inscrições e acompanhar o andamento de suas solicitações. Além disso, o sistema contará com funcionalidades administrativas para o gerenciamento de programas, editais, documentos e análise das submissões.

O projeto está sendo desenvolvido utilizando **Java e Spring Boot**, seguindo uma arquitetura em camadas e utilizando o padrão de persistência de dados com JPA.

---

## 🎯 Objetivos do projeto

- Centralizar o gerenciamento de programas de bolsas e auxílios da UPE.
- Facilitar o acesso dos estudantes aos editais disponíveis.
- Organizar o processo de inscrição e envio de documentos.
- Permitir o acompanhamento do status das submissões.
- Oferecer ferramentas administrativas para gerenciamento e análise das inscrições.
- Manter um histórico das alterações realizadas nas solicitações.

## 👥 Perfis de usuários

O sistema prevê diferentes perfis de acesso, com permissões específicas:

| Perfil | Responsabilidades |
|---|---|
| **Aluno** | Consultar editais, realizar inscrições, anexar documentos e acompanhar suas submissões. |
| **Professor** | Atuar como orientador nas submissões que exigem orientação acadêmica. |
| **Gerenciador** | Administrar programas, editais, inscrições, documentos e alterações de status. |

## ⚙️ Funcionalidades previstas

### Para estudantes
- Cadastro e autenticação no sistema.
- Consulta de programas de bolsas e auxílios.
- Visualização de editais e seus requisitos.
- Realização e envio de inscrições.
- Preenchimento de informações e respostas solicitadas pelos editais.
- Anexação de documentos comprobatórios.
- Consulta do protocolo e acompanhamento do status da inscrição.
- Recebimento de notificações sobre atualizações.

### Para gerenciadores
- Cadastro e gerenciamento de programas de bolsas e auxílios.
- Publicação e gerenciamento de editais.
- Definição de documentos e informações exigidos em cada edital.
- Consulta e análise das inscrições recebidas.
- Validação de documentos anexados.
- Atualização do status das submissões.
- Consulta ao histórico de alterações.
- Exportação de informações das inscrições.

## 🏗️ Arquitetura do projeto

O projeto segue uma **arquitetura em camadas**, buscando separar as responsabilidades e facilitar a manutenção, organização e evolução do código.

A estrutura está organizada da seguinte maneira:

```text
src/
└── main/
    └── java/
        └── br/
            └── edu/
                └── upe/
                    └── sgba/
                        ├── config/
                        ├── controller/
                        ├── dto/
                        ├── entity/
                        ├── exception/
                        ├── repository/
                        ├── service/
                        └── SgbaApplication.java
```

### Responsabilidades das camadas

| Camada | Responsabilidade |
|---|---|
| `config` | Centralizar configurações gerais da aplicação. |
| `controller` | Receber e tratar as requisições HTTP. |
| `dto` | Definir objetos utilizados na transferência de dados. |
| `entity` | Representar as entidades e o mapeamento das tabelas do banco de dados. |
| `exception` | Organizar o tratamento de exceções da aplicação. |
| `repository` | Responsável pelo acesso e persistência dos dados. |
| `service` | Concentrar as regras de negócio e operações da aplicação. |

## 🗃️ Modelagem de dados

A modelagem do banco de dados foi estruturada para representar os principais elementos envolvidos no gerenciamento de bolsas e auxílios.

As entidades inicialmente definidas são:

| Entidade | Descrição |
|---|---|
| `Usuario` | Armazena os dados de identificação e acesso dos usuários. |
| `Aluno` | Representa as informações acadêmicas e pessoais dos estudantes. |
| `ProgramaBolsa` | Representa os programas de bolsas e auxílios oferecidos. |
| `Edital` | Armazena os dados dos editais vinculados aos programas. |
| `ExigenciaEdital` | Define os documentos exigidos em cada edital. |
| `PerguntaEdital` | Armazena as perguntas adicionais de um edital. |
| `Submissao` | Representa uma inscrição realizada por um estudante. |
| `RespostaSubmissao` | Armazena as respostas fornecidas pelo estudante. |
| `DocumentoAnexo` | Representa os documentos enviados durante uma inscrição. |
| `HistoricoStatus` | Registra as alterações de status das submissões. |
| `Notificacao` | Armazena as notificações destinadas aos usuários. |

As entidades utilizam relacionamentos JPA para representar as associações definidas na modelagem, incluindo relacionamentos `OneToOne`, `OneToMany` e `ManyToOne`.

## 🛠️ Tecnologias utilizadas

- **Java 21** — Linguagem de programação.
- **Spring Boot** — Framework para desenvolvimento da aplicação.
- **Spring Data JPA** — Abstração para persistência e acesso aos dados.
- **Hibernate** — Implementação de ORM utilizada pelo JPA.
- **PostgreSQL** — Sistema gerenciador de banco de dados.
- **Maven** — Gerenciamento de dependências e construção do projeto.
- **Lombok** — Redução de código repetitivo nas classes Java.
- **Jakarta Validation** — Suporte à validação de dados.

## 🚧 Estado atual do desenvolvimento

O projeto encontra-se em fase inicial de desenvolvimento.

Até o momento, esta etapa contempla:

- Definição da arquitetura em camadas.
- Organização dos pacotes da aplicação.
- Criação das entidades JPA.
- Definição dos relacionamentos entre as entidades.
- Criação dos enums utilizados na modelagem.

As próximas etapas envolvem a implementação dos repositórios, DTOs, serviços, controladores, regras de negócio e demais funcionalidades previstas nos requisitos.

## ▶️ Como executar o projeto

### Pré-requisitos

Antes de executar a aplicação, é necessário ter instalado:

- **JDK 21** (o projeto compila com `--release 21`; um JRE não serve — precisa do `javac`).
- Docker + Docker Compose (para o banco do profile `dev`).
- Uma IDE de sua preferência, como IntelliJ IDEA, Eclipse ou VS Code.

> **Dica:** se o `javac` não estiver no PATH, aponte o `JAVA_HOME` para um JDK 21 (por exemplo `~/.jdks/jdk-21.x`) antes de usar o wrapper: `export JAVA_HOME=$HOME/.jdks/jdk-21.0.13+9`.
> Se `./mvnw` falhar com "permissão negada": `chmod +x mvnw`.

### Clonando o repositório

```bash
git clone URL_DO_REPOSITORIO
```

Acesse a pasta do projeto:

```bash
cd SGBA
```

### Configuração do banco de dados

Crie um banco de dados PostgreSQL para o projeto e configure as credenciais de acesso no arquivo `application.properties` ou `application.yml`, conforme a configuração utilizada na aplicação.

Exemplo:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/sgba
spring.datasource.username=SEU_USUARIO
spring.datasource.password=SUA_SENHA

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

**Importante:** não compartilhe senhas ou credenciais reais no repositório. Utilize variáveis de ambiente ou configurações locais para informações sensíveis.

### Executando a aplicação

Com o banco de dados configurado, execute:

```bash
./mvnw spring-boot:run
```

No Windows:

```bash
mvnw.cmd spring-boot:run
```

---

## 🚀 Execução rápida (Checkpoint 3 — Issue 1: Fundação)

### 1. Subir o PostgreSQL (profile `dev`)

O `compose.yaml` sobe o PostgreSQL **16** (db `sgba`, usuário `sgba`, senha `sgba`, porta `5432`,
com healthcheck e volume persistente):

```bash
docker compose up -d
```

Confirme que ficou saudável: `docker compose ps` (STATUS `healthy`).

### 2. Rodar a aplicação com o profile `dev`

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

No Windows:

```bash
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

O app cria o schema (`ddl-auto=update`) e, na primeira carga, o **seed de desenvolvimento**:
procure no log por `[DevSeedData] Seed aplicado` (com o contrato de IDs/e-mails/editais impresso).

### 3. Recarregar o seed

O seed é **idempotente** (se já existirem usuários, só registra no log). Para recarregar do zero:

```bash
docker compose down -v      # remove o volume com os dados
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. Smoke test da base de erros (D2)

Qualquer rota não mapeada responde **404** com `ErrorResponse`:

```bash
curl -i http://localhost:8080/api/inexistente
# {"status":404,"erro":"RECURSO_NAO_ENCONTRADO","mensagem":"Rota não mapeada na aplicação"}
```

### 5. Rodar os testes automatizados (profile `test`, H2 in-memory — não precisa do PG)

```bash
./mvnw test
```

### Dados do seed (contrato público para as Issues 2–6)

| Dado | Valor |
|---|---|
| Usuários | `aluno@upe.br` / `professor@upe.br` / `gerenciador@upe.br` (header `X-Usuario-Id` — D1) |
| Programa | "Programa de Iniciacao Cientifica (PIBIC)" — janela aberta (aparece em `/api/programas/abertos`) |
| Edital A | `08/2026` — janela aberta, 2 perguntas (1 obrigatória), 2 exigências **opcionais** → fluxo feliz (Issue 5) |
| Edital B | `09/2026` — janela aberta, 1 pergunta obrigatória, exigências **obrigatórias** → RN12 (Issue 6) |
| Edital C | `07/2025` — janela **encerrada** → RN05 `FORA_DO_PRAZO` 409 (Issue 5) |
| Edital D | `10/2026` — aberto, sem formulário → infra da Issue 4 (perguntas/exigências/duplicata 409) |

Os IDs exatos são impressos no log do seed — em banco limpo: usuários 1, 2, 3; programa 1; editais 1–4 (sequência IDENTITY por tabela).

---

## 📌 Sobre

O SGBA é um projeto acadêmico voltado ao desenvolvimento de uma solução para gerenciamento de bolsas e auxílios no contexto da Universidade de Pernambuco (UPE).

O desenvolvimento é orientado por requisitos funcionais e não funcionais e por uma modelagem de dados previamente definida.

**Projeto:** Sistema de Gerenciamento de Bolsas e Auxílios da UPE (SGBA).

**Instituição:** Universidade de Pernambuco (UPE).

**Área:** Desenvolvimento de Software / Engenharia de Software.
