package br.edu.upe.sgba.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.upe.sgba.dto.HistoricoResponse;
import br.edu.upe.sgba.dto.SubmissaoDetalheResponse;
import br.edu.upe.sgba.dto.SubmissaoEnviadaResponse;
import br.edu.upe.sgba.dto.SubmissaoPainelResponse;
import br.edu.upe.sgba.dto.SubmissaoRascunhoRequest;
import br.edu.upe.sgba.dto.SubmissaoUpdateRequest;
import br.edu.upe.sgba.service.SubmissaoService;
import lombok.RequiredArgsConstructor;

/**
 * Endpoints do aluno (RF07/RF11) — D1: usuário corrente no header
 * {@code X-Usuario-Id}; o Service valida existência/função/dono (RN06/§9.2).
 */
@RestController
@RequestMapping("/api/submissoes")
@RequiredArgsConstructor
public class SubmissaoController {

    private final SubmissaoService service;

    @PostMapping
    public ResponseEntity<SubmissaoDetalheResponse> criar(
            @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
            @RequestBody SubmissaoRascunhoRequest req) {
        SubmissaoDetalheResponse criada = service.criar(usuarioId, req);
        return ResponseEntity.created(URI.create("/api/submissoes/" + criada.id())).body(criada);
    }

    @GetMapping("/minhas")
    public List<SubmissaoPainelResponse> minhas(
            @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId) {
        return service.minhas(usuarioId);
    }

    @GetMapping("/{id}")
    public SubmissaoDetalheResponse detalhe(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId) {
        return service.detalhe(id, usuarioId);
    }

    @PutMapping("/{id}")
    public SubmissaoDetalheResponse atualizar(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
            @RequestBody SubmissaoUpdateRequest req) {
        return service.atualizar(id, usuarioId, req);
    }

    /** Envia o rascunho (RN02/RN05/RN09/RN12) — 200 (o recurso já existe). */
    @PostMapping("/{id}/enviar")
    public SubmissaoEnviadaResponse enviar(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId) {
        return service.enviar(id, usuarioId);
    }

    /** Timeline RF12 (append-only), mais recente primeiro. */
    @GetMapping("/{id}/historico")
    public List<HistoricoResponse> historico(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId) {
        return service.historico(id, usuarioId);
    }
}
