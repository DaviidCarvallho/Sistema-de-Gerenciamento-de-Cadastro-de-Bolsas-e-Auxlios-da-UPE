# SGBA — Arquitetura inicial e entidades

Projeto Spring Boot organizado em camadas, com base no DRS e no DER fornecidos.

## Pacote-base
`br.edu.upe.sgba`

## Camadas
- `controller`: entrada e saída HTTP (estrutura reservada).
- `service`: regras de negócio (estrutura reservada).
- `repository`: persistência com Spring Data JPA (estrutura reservada).
- `dto`: objetos de transferência (estrutura reservada).
- `entity`: entidades JPA e enums.
- `config`: configurações da aplicação (estrutura reservada).
- `exception`: tratamento centralizado de exceções (estrutura reservada).

## Entidades criadas
`Usuario`, `Aluno`, `ProgramaBolsa`, `Edital`, `ExigenciaEdital`, `PerguntaEdital`, `Submissao`, `RespostaSubmissao`, `DocumentoAnexo`, `HistoricoStatus` e `Notificacao`.

## Observações de modelagem
- `Aluno` compartilha a chave primária com `Usuario` por meio de `@MapsId`, refletindo a relação 1:1 do DER.
- `Submissao.aluno` e `Submissao.orientador` apontam para `Usuario`, conforme as chaves do DER.
- Os atributos de enum são persistidos como texto com `EnumType.STRING`.
- Os pacotes das camadas ainda não contêm implementações; foram criados apenas para estabelecer a arquitetura solicitada.
- Não foram implementados endpoints, regras de negócio, repositories, services ou DTOs.
