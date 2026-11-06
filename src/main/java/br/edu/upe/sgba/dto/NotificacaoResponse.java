package br.edu.upe.sgba.dto;

import java.time.LocalDateTime;

import br.edu.upe.sgba.entity.Notificacao;

/** Item do inbox do aluno (RF17): mensagem + link para a submissão (RF12). */
public record NotificacaoResponse(Long id, String mensagem, LocalDateTime criadoEm, boolean lida,
        Long submissaoId) {

    public static NotificacaoResponse from(Notificacao n) {
        return new NotificacaoResponse(n.getId(), n.getMensagem(), n.getCriadoEm(),
                n.getLidaEm() != null, n.getSubmissao() == null ? null : n.getSubmissao().getId());
    }
}
