package br.edu.upe.sgba.controller;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UsuarioAlunoApiTest {

    @LocalServerPort
    private int porta;

    private RestTestClient rest;

    @BeforeEach
    void setUp() {
        rest = RestTestClient.bindToServer().baseUrl("http://localhost:" + porta).build();
    }

    private static String emailUnico() {
        return "t" + UUID.randomUUID().toString().substring(0, 8) + "@upe.br";
    }

    private static String numeroUnico(long limite) {
        return String.valueOf(ThreadLocalRandom.current().nextLong(limite / 10, limite));
    }

    private static String jsonUsuario(String email, String funcao) {
        return "{\"nome\":\"Teste\",\"email\":\"" + email + "\",\"senha\":\"123456\",\"funcao\":\"" + funcao + "\"}";
    }

    private long criarUsuario(String email, String funcao) {
        String corpo = rest.post().uri("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonUsuario(email, funcao))
                .exchange()
                .expectStatus().isEqualTo(201)
                .returnResult(String.class).getResponseBody();
        Matcher m = Pattern.compile("\"id\":(\\d+)").matcher(corpo);
        assertTrue(m.find());
        return Long.parseLong(m.group(1));
    }

    private String jsonAluno(long usuarioId, String cpf, String matricula) {
        return "{\"usuarioId\":" + usuarioId + ",\"cpf\":\"" + cpf + "\",\"matricula\":\"" + matricula
                + "\",\"curso\":\"Sistemas de Informacao\",\"craAtual\":8.5,"
                + "\"endereco\":{\"cidade\":\"Recife\",\"uf\":\"PE\"}}";
    }

    @Test
    void criarUsuario_valido_retorna201ComLocationESemSenha() {
        String email = emailUnico();
        rest.post().uri("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                .body(jsonUsuario(email, "ALUNO"))
                .exchange()
                .expectStatus().isEqualTo(201)
                .expectHeader().exists("Location")
                .expectBody()
                .jsonPath("$.email").isEqualTo(email)
                .jsonPath("$.senha").doesNotExist()
                .jsonPath("$.senhaHash").doesNotExist();
    }

    @Test
    void criarUsuario_emailForaDoDominio_retorna400Rn01() {
        rest.post().uri("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                .body(jsonUsuario("fulano@gmail.com", "ALUNO"))
                .exchange()
                .expectStatus().isEqualTo(400)
                .expectBody()
                .jsonPath("$.erro").isEqualTo("EMAIL_INSTITUCIONAL_INVALIDO");
    }

    @Test
    void criarUsuario_emailDuplicado_retorna409() {
        String email = emailUnico();
        criarUsuario(email, "ALUNO");
        rest.post().uri("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                .body(jsonUsuario(email, "ALUNO"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.erro").isEqualTo("EMAIL_JA_CADASTRADO");
    }

    @Test
    void criarUsuario_funcaoInvalida_retorna400() {
        rest.post().uri("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                .body(jsonUsuario(emailUnico(), "DIRETOR"))
                .exchange()
                .expectStatus().isEqualTo(400)
                .expectBody()
                .jsonPath("$.erro").isEqualTo("ARGUMENTO_INVALIDO");
    }

    @Test
    void buscarUsuarioInexistente_retorna404() {
        rest.get().uri("/api/usuarios/999999")
                .exchange()
                .expectStatus().isEqualTo(404)
                .expectBody()
                .jsonPath("$.erro").isEqualTo("RECURSO_NAO_ENCONTRADO");
    }

    @Test
    void listarProfessores_retornaApenasProfessores() {
        String email = emailUnico();
        criarUsuario(email, "PROFESSOR");
        rest.get().uri("/api/usuarios?funcao=PROFESSOR")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[?(@.email=='" + email + "')]").exists();
    }

    @Test
    void criarAluno_valido_usaMesmoIdDoUsuarioEBloqueiaDuplicatas() {
        long uid = criarUsuario(emailUnico(), "ALUNO");
        String cpf = numeroUnico(100_000_000_000L);
        String matricula = numeroUnico(100_000_000L);

        rest.post().uri("/api/alunos").contentType(MediaType.APPLICATION_JSON)
                .body(jsonAluno(uid, cpf, matricula))
                .exchange()
                .expectStatus().isEqualTo(201)
                .expectHeader().exists("Location")
                .expectBody()
                .jsonPath("$.usuarioId").isEqualTo(uid);

        // mesmo usuário de novo → 409 ALUNO_JA_EXISTENTE
        rest.post().uri("/api/alunos").contentType(MediaType.APPLICATION_JSON)
                .body(jsonAluno(uid, cpf, matricula))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.erro").isEqualTo("ALUNO_JA_EXISTENTE");

        // outro usuário com o mesmo CPF → 409 CPF_JA_CADASTRADO
        long outro = criarUsuario(emailUnico(), "ALUNO");
        rest.post().uri("/api/alunos").contentType(MediaType.APPLICATION_JSON)
                .body(jsonAluno(outro, cpf, numeroUnico(100_000_000L)))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.erro").isEqualTo("CPF_JA_CADASTRADO");
    }

    @Test
    void criarAluno_paraUsuarioProfessor_retorna400PermissaoInsuficiente() {
        long prof = criarUsuario(emailUnico(), "PROFESSOR");
        rest.post().uri("/api/alunos").contentType(MediaType.APPLICATION_JSON)
                .body(jsonAluno(prof, numeroUnico(100_000_000_000L), numeroUnico(100_000_000L)))
                .exchange()
                .expectStatus().isEqualTo(400)
                .expectBody().jsonPath("$.erro").isEqualTo("PERMISSAO_INSUFICIENTE");
    }

    @Test
    void buscarAlunoInexistente_retorna404() {
        rest.get().uri("/api/alunos/999999")
                .exchange()
                .expectStatus().isEqualTo(404);
    }
}