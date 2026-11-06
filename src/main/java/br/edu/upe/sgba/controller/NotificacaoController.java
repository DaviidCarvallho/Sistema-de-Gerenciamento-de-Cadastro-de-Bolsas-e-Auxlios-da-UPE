package br.edu.upe.sgba.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.upe.sgba.dto.NotificacaoResponse;
import br.edu.upe.sgba.service.NotificacaoService;
import lombok.RequiredArgsConstructor;

/**
 * Inbox de notificações (RF17) — D1: destinatário vem do header
 * {@code X-Usuario-Id}; o Service valida existência (sem auth no cp3).
 */
@RestController
@RequestMapping("/api/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoService service;

    @GetMapping
    public List<NotificacaoResponse> listar(
            @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
            @RequestParam(required = false) Boolean somenteNaoLidas) {
        return service.listar(usuarioId, Boolean.TRUE.equals(somenteNaoLidas));
    }

    /** Marca como lida — 200 + body atualizado (padrão da Issue 6); idempotente. */
    @PatchMapping("/{id}/lida")
    public ResponseEntity<NotificacaoResponse> marcarLida(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId) {
        return ResponseEntity.ok(service.marcarLida(id, usuarioId));
    }
}
