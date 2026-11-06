package br.edu.upe.sgba.controller;

import br.edu.upe.sgba.dto.ProgramaUpsertRequest;
import br.edu.upe.sgba.dto.ProgramaResponse;
import br.edu.upe.sgba.dto.ProgramaCatalogoResponse;
import br.edu.upe.sgba.entity.enums.CategoriaPrograma;
import br.edu.upe.sgba.entity.enums.StatusPrograma;
import br.edu.upe.sgba.service.ProgramaBolsaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/programas")
public class ProgramaBolsaController {

    private final ProgramaBolsaService service;

    public ProgramaBolsaController(ProgramaBolsaService service) {
        this.service = service;
    }

    // LISTAR COM FILTROS
    @GetMapping
    public ResponseEntity<Page<ProgramaResponse>> listar(@RequestParam(required = false) CategoriaPrograma categoria, @RequestParam(required = false) String q,
                                                         @RequestParam(required = false) StatusPrograma status,
                                                         @PageableDefault(size = 10, sort = "id") Pageable pageable) {

        return ResponseEntity.ok(service.listarComFiltros(categoria, q, status, pageable));
    }

    // CATÁLOGO PÚBLICO
    @GetMapping("/abertos")
    public ResponseEntity<List<ProgramaCatalogoResponse>> listarAbertos() {

        return ResponseEntity.ok(service.listarAbertos());
    }

    // BUSCAR POR ID
    @GetMapping("/{id}")
    public ResponseEntity<ProgramaResponse> buscar(@PathVariable Long id) {

        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // CADASTRAR
    @PostMapping
    public ResponseEntity<ProgramaResponse> cadastrar(@RequestBody ProgramaUpsertRequest request) {
        ProgramaResponse response = service.cadastrar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();

        return ResponseEntity.created(location).body(response);
    }

    // ATUALIZAR
    @PutMapping("/{id}")
    public ResponseEntity<ProgramaResponse> atualizar(@PathVariable Long id, @RequestBody ProgramaUpsertRequest request) {

        return ResponseEntity.ok(service.atualizar(id, request));
    }

    // ENCERRAR
    @PostMapping("/{id}/encerrar")
    public ResponseEntity<ProgramaResponse> encerrar(@PathVariable Long id) {

        return ResponseEntity.ok(service.encerrar(id));
    }
}