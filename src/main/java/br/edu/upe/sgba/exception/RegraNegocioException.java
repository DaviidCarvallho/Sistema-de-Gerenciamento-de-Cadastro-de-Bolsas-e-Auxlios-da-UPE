package br.edu.upe.sgba.exception;

import org.springframework.http.HttpStatus;

/**
 * Violação de regra de negócio (RNs do DRS), lançada pela camada Service.
 *
 * <p>Convenção D2 (Checkpoint 3): carrega o status HTTP que o
 * {@code GlobalExceptionHandler} devolve — <b>default 409 CONFLICT</b>
 * (conflito de unicidade/regra). Para regras de "argumento inválido"
 * (ex.: RN01 — e-mail fora de domínio, RN04 — motivo obrigatório, janelas
 * inválidas) usa-se 400 BAD_REQUEST.</p>
 *
 * <p>Exemplo de criação:</p>
 * <pre>{@code
 * throw new RegraNegocioException("SUBMISSAO_DUPLICADA", "Aluno já possui submissão para este edital");
 *          // → 409
 * throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "EMAIL_INSTITUCIONAL_INVALIDO", "O e-mail deve terminar em @upe.br");
 *          // → 400
 * }</pre>
 */
public class RegraNegocioException extends RuntimeException {

    /** Código curto da regra para o body de erro (ex.: FORA_DO_PRAZO). */
    private final String codigoErro;

    /** Status HTTP a devolver (409 por padrão; 400 para regras de argumento inválido). */
    private final HttpStatus status;

    public RegraNegocioException(String codigoErro, String mensagem) {
        this(HttpStatus.CONFLICT, codigoErro, mensagem);
    }

    public RegraNegocioException(HttpStatus status, String codigoErro, String mensagem) {
        super(mensagem);
        this.codigoErro = codigoErro;
        this.status = status;
    }

    public String getCodigoErro() {
        return codigoErro;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
