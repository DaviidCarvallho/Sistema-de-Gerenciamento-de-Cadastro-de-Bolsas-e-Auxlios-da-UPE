package br.edu.upe.sgba.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.upe.sgba.dto.DocumentoDownload;
import br.edu.upe.sgba.dto.DocumentoResponse;
import br.edu.upe.sgba.dto.DocumentoValidacaoRequest;
import br.edu.upe.sgba.service.DocumentoAnexoService;
import lombok.RequiredArgsConstructor;

/**
 * Documentos por id (Issue 6): download dos bytes (RF09), delete (RN03) e
 * validação do anexo pelo gerenciador (RF15, §9.2). As rotas aninhadas na
 * submissão ficam em {@link SubmissaoController}.
 */
@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoAnexoController {

    private final DocumentoAnexoService service;

    /** Download do arquivo — 200 application/pdf com os mesmos bytes do upload. */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId) {
        DocumentoDownload d = service.baixar(id, usuarioId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + d.nomeOriginal() + "\"")
                .body(d.conteudo());
    }

    /** RN03 — só em RASCUNHO/PENDENTE_DOCUMENTOS e pelo dono → 204. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId) {
        service.excluir(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    /** RF15 — validação do anexo pelo gerenciador (§9.2): 200 / 400 / 404. */
    @PatchMapping("/{id}/status")
    public DocumentoResponse validarStatus(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
            @RequestBody DocumentoValidacaoRequest req) {
        return service.validarStatus(id, usuarioId, req);
    }
}
