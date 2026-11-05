package br.edu.upe.sgba.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Critério de aceite da Issue 1: {@code GET /api/inexistente} → <b>404</b> com
 * body no formato {@code ErrorResponse} (status, erro, mensagem) — prova do
 * handler base D2. Integração real: Tomcat de porta aleatória + profile test (H2).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RotaInexistenteTest {

    @LocalServerPort
    private int porta;

    private RestTestClient rest;

    @BeforeEach
    void setUp() {
        rest = RestTestClient.bindToServer().baseUrl("http://localhost:" + porta).build();
    }

    @Test
    void rotaInexistente_deveRetornar404ComCorpoErrorResponse() {
        rest.get().uri("/api/inexistente")
                .exchange()
                .expectStatus().isEqualTo(404)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.erro").isEqualTo("RECURSO_NAO_ENCONTRADO")
                .jsonPath("$.mensagem").isNotEmpty();
    }
}
