package br.edu.upe.sgba.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.upe.sgba.dto.AlunoResponse;
import br.edu.upe.sgba.dto.AlunoUpsertRequest;
import br.edu.upe.sgba.service.AlunoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/alunos")
@RequiredArgsConstructor
public class AlunoController {

    private final AlunoService service;

    @GetMapping("/{usuarioId}")
    public AlunoResponse buscar(@PathVariable Long usuarioId) {
        return service.buscar(usuarioId);
    }

    @PostMapping
    public ResponseEntity<AlunoResponse> criar(@RequestBody AlunoUpsertRequest req) {
        AlunoResponse criado = service.criar(req);
        return ResponseEntity.created(URI.create("/api/alunos/" + criado.usuarioId())).body(criado);
    }

    @PutMapping("/{usuarioId}")
    public AlunoResponse atualizar(@PathVariable Long usuarioId, @RequestBody AlunoUpsertRequest req) {
        return service.atualizar(usuarioId, req);
    }
}