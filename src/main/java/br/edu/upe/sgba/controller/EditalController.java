package br.edu.upe.sgba.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.edu.upe.sgba.dto.ArquivoEditalResponse;
import br.edu.upe.sgba.dto.EditalComFormularioResponse;
import br.edu.upe.sgba.dto.EditalResponse;
import br.edu.upe.sgba.dto.EditalUpsertRequest;
import br.edu.upe.sgba.service.EditalService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/editais")
@RequiredArgsConstructor
public class EditalController {

    private final EditalService service;

    @GetMapping
    public Page<EditalResponse> listar(@RequestParam(required = false) Long programaId, Pageable pageable) {
        return service.listar(programaId, pageable);
    }

    @GetMapping("/{id}")
    public EditalResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @GetMapping("/{id}/formulario")
    public EditalComFormularioResponse formulario(@PathVariable Long id) {
        return service.formulario(id);
    }

    @PostMapping
    public ResponseEntity<EditalResponse> criar(@RequestBody EditalUpsertRequest req) {
        EditalResponse criado = service.criar(req);
        return ResponseEntity.created(URI.create("/api/editais/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public EditalResponse atualizar(@PathVariable Long id, @RequestBody EditalUpsertRequest req) {
        return service.atualizar(id, req);
    }

    @PostMapping(path = "/{id}/arquivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArquivoEditalResponse> enviarArquivo(
            @PathVariable Long id,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        ArquivoEditalResponse r = service.anexarArquivo(id, file);
        return ResponseEntity.created(URI.create("/api/editais/" + id + "/arquivo")).body(r);
    }

    @GetMapping("/{id}/arquivo")
    public ResponseEntity<byte[]> baixarArquivo(@PathVariable Long id) {
        byte[] pdf = service.baixarArquivo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"edital-" + id + ".pdf\"")
                .body(pdf);
    }
}