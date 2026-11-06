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

import br.edu.upe.sgba.dto.DecisaoRequest;
import br.edu.upe.sgba.dto.DocumentoResponse;
import br.edu.upe.sgba.dto.HistoricoResponse;
import br.edu.upe.sgba.dto.SubmissaoDetalheResponse;
import br.edu.upe.sgba.dto.SubmissaoEnviadaResponse;
import br.edu.upe.sgba.dto.SubmissaoPainelResponse;
import br.edu.upe.sgba.dto.SubmissaoRascunhoRequest;
import br.edu.upe.sgba.dto.SubmissaoUpdateRequest;
import br.edu.upe.sgba.service.DocumentoAnexoService;
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
    private final DocumentoAnexoService documentoService;

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

    /** RF16 pelo gerenciador (§9.2) — requer função GERENCIADOR; 409 transição, 400 motivo. */
    @org.springframework.web.bind.annotation.PatchMapping("/{id}/status")
    public SubmissaoDetalheResponse mudarStatus(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
            @RequestBody DecisaoRequest req) {
        return service.mudarStatus(id, usuarioId, req);
    }

    // ------------------------------ documentos da submissão (Issue 6) ------------------------------

    /** RF09 — upload multipart: campo 'arquivo' + 'tipoDocumento' (+ descricaoPersonalizada se OUTRO). */
    @PostMapping(path = "/{id}/documentos", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponse> anexarDocumento(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
            @org.springframework.web.bind.annotation.RequestPart(value = "arquivo", required = false) org.springframework.web.multipart.MultipartFile arquivo,
            @org.springframework.web.bind.annotation.RequestParam("tipoDocumento") String tipoDocumento,
            @org.springframework.web.bind.annotation.RequestParam(value = "descricaoPersonalizada", required = false) String descricaoPersonalizada) {
        DocumentoResponse criado = documentoService.anexar(id, usuarioId, arquivo, tipoDocumento,
                descricaoPersonalizada);
        return ResponseEntity.created(URI.create("/api/documentos/" + criado.id())).body(criado);
    }

    /** Metadados dos anexos (RF15) — dono OU gerenciador (RN06). */
    @GetMapping("/{id}/documentos")
    public List<DocumentoResponse> listarDocumentos(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId) {
        return documentoService.listar(id, usuarioId);
    }
}
