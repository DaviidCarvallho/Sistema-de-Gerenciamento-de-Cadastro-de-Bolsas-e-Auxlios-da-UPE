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

- Java JDK 21 ou superior compatível com o projeto.
- Maven.
- PostgreSQL.
- Uma IDE de sua preferência, como IntelliJ IDEA, Eclipse ou VS Code.

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

## 📌 Sobre

O SGBA é um projeto acadêmico voltado ao desenvolvimento de uma solução para gerenciamento de bolsas e auxílios no contexto da Universidade de Pernambuco (UPE).

O desenvolvimento é orientado por requisitos funcionais e não funcionais e por uma modelagem de dados previamente definida.

**Projeto:** Sistema de Gerenciamento de Bolsas e Auxílios da UPE (SGBA).

**Instituição:** Universidade de Pernambuco (UPE).

**Área:** Desenvolvimento de Software / Engenharia de Software.
