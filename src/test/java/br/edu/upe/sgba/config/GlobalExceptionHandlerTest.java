package br.edu.upe.sgba.config;

import br.edu.upe.sgba.exception.ErrorResponse;
import br.edu.upe.sgba.exception.RecursoNaoEncontradoException;
import br.edu.upe.sgba.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unitária do handler base D2: cada exceção do domínio mapeia para o status
 * e o contrato de body acordado (status, erro, mensagem).
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void recursoNaoEncontrado_deveMapear404() {
        ResponseEntity<ErrorResponse> resp = handler.handleRecursoNaoEncontrado(
                RecursoNaoEncontradoException.porId("Submissao", 7L));

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals(404, resp.getBody().status());
        assertEquals("RECURSO_NAO_ENCONTRADO", resp.getBody().erro());
        assertEquals("Submissao não encontrado(a) para o id 7", resp.getBody().mensagem());
    }

    @Test
    void regraNegocio_deveUsarStatusDaExcecao_409PorPadrao() {
        RegraNegocioException ex = new RegraNegocioException("SUBMISSAO_DUPLICADA",
                "Aluno já possui submissão para este edital");

        ResponseEntity<ErrorResponse> resp = handler.handleRegraNegocio(ex);

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertEquals(409, resp.getBody().status());
        assertEquals("SUBMISSAO_DUPLICADA", resp.getBody().erro());
    }

    @Test
    void regraNegocio_argumentoInvalido_deveMapear400() {
        RegraNegocioException ex = new RegraNegocioException(HttpStatus.BAD_REQUEST,
                "EMAIL_INSTITUCIONAL_INVALIDO", "O e-mail deve terminar em @upe.br");

        ResponseEntity<ErrorResponse> resp = handler.handleRegraNegocio(ex);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(400, resp.getBody().status());
        assertEquals("EMAIL_INSTITUCIONAL_INVALIDO", resp.getBody().erro());
    }

    @Test
    void rotaInexistente_deveMapear404ComErrorResponse() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/api/inexistente", "/api/inexistente");

        ResponseEntity<ErrorResponse> resp = handler.handleRotaInexistente(ex);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals(404, resp.getBody().status());
        assertEquals("RECURSO_NAO_ENCONTRADO", resp.getBody().erro());
    }
}
