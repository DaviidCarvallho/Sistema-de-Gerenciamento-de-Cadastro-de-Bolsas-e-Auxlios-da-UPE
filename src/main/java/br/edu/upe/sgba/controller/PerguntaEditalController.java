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

import br.edu.upe.sgba.dto.PerguntaRequest;
import br.edu.upe.sgba.dto.PerguntaResponse;
import br.edu.upe.sgba.service.PerguntaEditalService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/editais/{editalId}/perguntas")
@RequiredArgsConstructor
public class PerguntaEditalController {

    private final PerguntaEditalService service;

    @PostMapping
    public ResponseEntity<PerguntaResponse> criar(@PathVariable Long editalId, @RequestBody PerguntaRequest req) {
        PerguntaResponse criada = service.criar(editalId, req);
        return ResponseEntity.created(
                URI.create("/api/editais/" + editalId + "/perguntas/" + criada.id())).body(criada);
    }

    @PutMapping("/{perguntaId}")
    public PerguntaResponse atualizar(@PathVariable Long editalId, @PathVariable Long perguntaId,
                                      @RequestBody PerguntaRequest req) {
        return service.atualizar(editalId, perguntaId, req);
    }

    @DeleteMapping("/{perguntaId}")
    public ResponseEntity<Void> remover(@PathVariable Long editalId, @PathVariable Long perguntaId) {
        service.remover(editalId, perguntaId);
        return ResponseEntity.noContent().build();
    }
}