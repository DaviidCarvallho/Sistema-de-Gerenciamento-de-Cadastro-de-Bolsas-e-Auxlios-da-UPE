package br.edu.upe.sgba.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.upe.sgba.entity.Notificacao;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    /** Inbox RF17 (D1 — header do destinatário), mais recentes primeiro. */
    List<Notificacao> findByDestinatarioIdOrderByCriadoEmDesc(Long destinatarioId);

    /** Marcar-lida com ownership (PATCH de notificação de outro usuário → 404). */
    Optional<Notificacao> findByIdAndDestinatarioId(Long id, Long destinatarioId);

    /** Contador do inbox (somenteNaoLidas/RF17). */
    int countByDestinatarioIdAndLidaEmIsNull(Long destinatarioId);
}
