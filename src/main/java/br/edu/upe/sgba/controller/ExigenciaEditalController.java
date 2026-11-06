package br.edu.upe.sgba.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.upe.sgba.dto.ExigenciaRequest;
import br.edu.upe.sgba.dto.ExigenciaResponse;
import br.edu.upe.sgba.service.ExigenciaEditalService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/editais/{editalId}/exigencias")
@RequiredArgsConstructor
public class ExigenciaEditalController {

    private final ExigenciaEditalService service;

    @PostMapping
    public ResponseEntity<ExigenciaResponse> criar(@PathVariable Long editalId, @RequestBody ExigenciaRequest req) {
        ExigenciaResponse criada = service.criar(editalId, req);
        return ResponseEntity.created(
                URI.create("/api/editais/" + editalId + "/exigencias/" + criada.id())).body(criada);
    }

    @PutMapping("/{exigenciaId}")
    public ExigenciaResponse atualizar(@PathVariable Long editalId, @PathVariable Long exigenciaId,
                                       @RequestBody ExigenciaRequest req) {
        return service.atualizar(editalId, exigenciaId, req);
    }

    @DeleteMapping("/{exigenciaId}")
    public ResponseEntity<Void> remover(@PathVariable Long editalId, @PathVariable Long exigenciaId) {
        service.remover(editalId, exigenciaId);
        return ResponseEntity.noContent().build();
    }
}