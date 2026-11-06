package br.edu.upe.sgba.service.port;

/**
 * Porta de notificação (Issue 5 entrega a interface; Issue 6 conecta a
 * implementação — desacopla o módulo de Submissões do de Notificações).
 *
 * <p>Chamada pelo {@code SubmissaoService} em toda transição de status (RN07):
 * a implementação persiste a {@code Notificacao} destinada ao aluno (RF17)
 * <b>na mesma transação</b> da mudança de status.</p>
 */
public interface NotificacaoPort {

    /**
     * Registra uma notificação de transição de status.
     *
     * @param destinatarioId id do usuário destinatário (dono da submissão)
     * @param submissaoId    id da submissão relacionada (link RF17 → RF12)
     * @param mensagem       texto completo (protocolo, status antigo→novo, motivo)
     */
    void notificarTransicao(Long destinatarioId, Long submissaoId, String mensagem);
}
