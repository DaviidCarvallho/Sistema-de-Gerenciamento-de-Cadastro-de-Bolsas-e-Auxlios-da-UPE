package br.edu.upe.sgba.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NotificacaoApiTest {

    @LocalServerPort
    private int porta;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager tm;

    private RestTestClient rest;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        rest = RestTestClient.bindToServer().baseUrl("http://localhost:" + porta).build();
    }

    private Long criarUsuario(String sufixo) {
        String corpo = rest.post().uri("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"nome\":\"Aluno Teste\",\"email\":\"" + sufixo + "@upe.br\",\"senha\":\"x\",\"funcao\":\"ALUNO\"}")
                .exchange()
                .expectStatus()
                .isEqualTo(201)
                .returnResult(String.class)
                .getResponseBody();
        Matcher m = Pattern.compile("\"id\":(\\d+)").matcher(corpo);
        assertTrue(m.find());
        return Long.parseLong(m.group(1));
    }

    @Test
    void listar_inbox_vazio_retorna200() {
        Long usuarioId = criarUsuario("inbox" + System.nanoTime());
        rest.get().uri("/api/notificacoes")
                .header("X-Usuario-Id", usuarioId.toString())
                .exchange()
                .expectStatus()
                .isEqualTo(200)
                .expectBody()
                .jsonPath("$")
                .isArray()
                .jsonPath("$")
                .isEmpty();
    }

    @Test
    void semHeader_retorna400() {
        rest.get().uri("/api/notificacoes").exchange().expectStatus().isEqualTo(400)
                .expectBody().jsonPath("$.erro").isEqualTo("ARGUMENTO_INVALIDO");
    }

    @Test
    void marcarLida_idInexistente_retorna404() {
        Long usuarioId = criarUsuario("inboxnao" + System.nanoTime());
        rest.patch().uri("/api/notificacoes/999999/lida")
                .header("X-Usuario-Id", usuarioId.toString())
                .exchange()
                .expectStatus().isEqualTo(404)
                .expectBody()
                .jsonPath("$.erro")
                .isEqualTo("RECURSO_NAO_ENCONTRADO");
    }
}
