package br.edu.upe.sgba.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.upe.sgba.dto.NotificacaoResponse;
import br.edu.upe.sgba.entity.Notificacao;
import br.edu.upe.sgba.entity.Submissao;
import br.edu.upe.sgba.entity.Usuario;
import br.edu.upe.sgba.exception.RecursoNaoEncontradoException;
import br.edu.upe.sgba.exception.RegraNegocioException;
import br.edu.upe.sgba.repository.NotificacaoRepository;
import br.edu.upe.sgba.repository.SubmissaoRepository;
import br.edu.upe.sgba.repository.UsuarioRepository;
import br.edu.upe.sgba.service.port.NotificacaoPort;
import lombok.RequiredArgsConstructor;

/**
 * Inbox de notificações (RF17) — implementação da {@link NotificacaoPort}.
 *
 * <p>O hook é chamado pelo {@code SubmissaoService} a cada transição de status
 * (RN07) na mesma transação; {@link #notificarTransicao} apenas persiste.
 * {@code lista}/,{@code PATCH /lida} usam o header {@code X-Usuario-Id} (D1).</p>
 */
@Service
@RequiredArgsConstructor
public class NotificacaoService implements NotificacaoPort {

    private final NotificacaoRepository notificacoes;
    private final UsuarioRepository usuarios;
    private final SubmissaoRepository submissoes;

    @Override
    @Transactional
    public void notificarTransicao(Long destinatarioId, Long submissaoId, String mensagem) {
        if (destinatarioId == null) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "ARGUMENTO_INVALIDO",
                    "Usuário não informado (header X-Usuario-Id)");
        }
        Notificacao n = new Notificacao();
        n.setDestinatario(usuarios.getReferenceById(destinatarioId));
        n.setSubmissao(submissaoId == null ? null : submissoes.getReferenceById(submissaoId));
        n.setMensagem(mensagem);
        n.setCriadoEm(LocalDateTime.now());
        notificacoes.save(n);
    }

    /** RF17 — inbox do destinatário; {@code somenteNaoLidas} filtra as já lidas. */
    @Transactional(readOnly = true)
    public List<NotificacaoResponse> listar(Long destinatarioId, boolean somenteNaoLidas) {
        exigirUsuario(destinatarioId);
        List<Notificacao> lista = notificacoes.findByDestinatarioIdOrderByCriadoEmDesc(destinatarioId);
        if (somenteNaoLidas) {
            lista = lista.stream().filter(n -> n.getLidaEm() == null).collect(Collectors.toList());
        }
        return lista.stream().map(NotificacaoResponse::from).toList();
    }

    /** Marcar como lida (idempotente): já lida devolve a mesma notificação com 200. */
    @Transactional
    public NotificacaoResponse marcarLida(Long id, Long destinatarioId) {
        exigirUsuario(destinatarioId);
        Notificacao n = notificacoes.findByIdAndDestinatarioId(id, destinatarioId)
                .orElseThrow(() -> RecursoNaoEncontradoException.porId("Notificacao", id));
        if (n.getLidaEm() == null) {
            n.setLidaEm(LocalDateTime.now());
        }
        return NotificacaoResponse.from(n);
    }

    private void exigirUsuario(Long destinatarioId) {
        if (destinatarioId == null) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "ARGUMENTO_INVALIDO",
                    "Usuário não informado (header X-Usuario-Id)");
        }
        if (!usuarios.existsById(destinatarioId)) {
            throw RecursoNaoEncontradoException.porId("Usuario", destinatarioId);
        }
    }
}
