package br.edu.upe.sgba.exception;

/**
 * Indica que um recurso solicitado não existe (conteúdo via GET/PUT/PATCH/DELETE
 * de id inexistente). Mapeada pelo {@code GlobalExceptionHandler} para HTTP 404.
 *
 * <p>Convenção D2 (Checkpoint 3): recursos inexistentes respondem 404 com
 * {@link ErrorResponse} — nunca 200 com corpo vazio nem 500.</p>
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    /** Mensagem padrão: "Recurso não encontrado: <entidade> id <id>". */
    public static RecursoNaoEncontradoException porId(String entidade, Long id) {
        return new RecursoNaoEncontradoException("%s não encontrado(a) para o id %d".formatted(entidade, id));
    }
}
