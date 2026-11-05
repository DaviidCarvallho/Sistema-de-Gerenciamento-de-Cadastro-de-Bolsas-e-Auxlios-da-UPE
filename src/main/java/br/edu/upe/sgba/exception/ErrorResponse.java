package br.edu.upe.sgba.exception;

/**
 * Corpo padrão de erro (D2 — Checkpoint 3).
 *
 * <p>Formato acordado para todos os erros dos módulos:</p>
 * <pre>{@code
 * { "status": 409, "erro": "SUBMISSAO_DUPLICADA", "mensagem": "Aluno já possui submissão para este edital" }
 * }</pre>
 *
 * <p>O tratamento centralizado completo (@ControllerAdvice com Bean Validation,
 * handlers de autenticação etc.) é do Checkpoint 4; aqui fica a base mínima.</p>
 */
public record ErrorResponse(int status, String erro, String mensagem) {

    /** Fábrica a partir de uma {@link RecursoNaoEncontradoException} → 404. */
    public static ErrorResponse de(RecursoNaoEncontradoException ex) {
        return new ErrorResponse(404, "RECURSO_NAO_ENCONTRADO", ex.getMessage());
    }

    /** Fábrica a partir de uma {@link RegraNegocioException} (status da exceção). */
    public static ErrorResponse de(RegraNegocioException ex) {
        return new ErrorResponse(ex.getStatus().value(), ex.getCodigoErro(), ex.getMessage());
    }

    /** Fábrica para erros de argumento/payload inválido → 400. */
    public static ErrorResponse argumentoInvalido(String mensagem) {
        return new ErrorResponse(400, "ARGUMENTO_INVALIDO", mensagem);
    }
}
