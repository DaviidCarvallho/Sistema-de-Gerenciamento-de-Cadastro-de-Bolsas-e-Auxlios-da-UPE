package br.edu.upe.sgba.config;

import br.edu.upe.sgba.exception.ErrorResponse;
import br.edu.upe.sgba.exception.RecursoNaoEncontradoException;
import br.edu.upe.sgba.exception.RegraNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Base mínima de tratamento de erro (D2 — Checkpoint 3).
 *
 * <p>Mapeamentos:</p>
 * <ul>
 *   <li>Recurso inexistente ({@link RecursoNaoEncontradoException} e rotas não
 *       mapeadas — ex.: {@code GET /api/inexistente}) → <b>404</b>;</li>
 *   <li>Violação de regra de negócio/unicidade ({@link RegraNegocioException} e
 *       constraint violada) → <b>409</b> (ou <b>400</b> quando a regra for de
 *       argumento inválido — RN01, RN04, janelas);</li>
 *   <li>Argumento/payload inválido (tipo errado de path param, JSON malformado)
 *       → <b>400</b>.</li>
 * </ul>
 *
 * <p>O tratamento centralizado completo (Bean Validation robusto, handlers de
 * autenticação/autorização) é do Checkpoint 4 — esta base não disputa esses
 * critérios.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        log.debug("Recurso não encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.de(ex));
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErrorResponse> handleRegraNegocio(RegraNegocioException ex) {
        log.debug("Regra de negócio violada [{}]: {}", ex.getCodigoErro(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.de(ex));
    }

    /** Rota inexistente (qualquer caminho não mapeado) → 404 com ErrorCode padrão. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleRotaInexistente(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "RECURSO_NAO_ENCONTRADO", "Rota não mapeada na aplicação"));
    }

    /** Path param com tipo inválido (ex.: id recebendo texto) → 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipoParamInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.argumentoInvalido(
                        "Valor inválido para o parâmetro '%s'".formatted(ex.getName())));
    }

    /** Payload JSON malformado/ilegível → 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handlePayloadIlegivel(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.argumentoInvalido("Payload JSON malformado ou ilegível"));
    }

    /** Constraint violada (unicidade/FK não prevista como regra explícita) → 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleViolacaoIntegridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "VIOLACAO_DE_INTEGRIDADE",
                        "Operação viola unicidade ou integridade dos dados"));
    }
}
