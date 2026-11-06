package br.edu.upe.sgba.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.upe.sgba.dto.ProfessorResponse;
import br.edu.upe.sgba.dto.UsuarioCreateRequest;
import br.edu.upe.sgba.dto.UsuarioResponse;
import br.edu.upe.sgba.dto.UsuarioUpdateRequest;
import br.edu.upe.sgba.entity.enums.FuncaoUsuario;
import br.edu.upe.sgba.exception.RegraNegocioException;
import br.edu.upe.sgba.service.UsuarioService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService service;

    @GetMapping
    public List<ProfessorResponse> listar(@RequestParam(required = false) FuncaoUsuario funcao) {
        if (funcao == null) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "ARGUMENTO_INVALIDO",
                    "Informe o filtro ?funcao= (ALUNO, PROFESSOR ou GERENCIADOR)");
        }
        return service.listarPorFuncao(funcao);
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@RequestBody UsuarioCreateRequest req) {
        UsuarioResponse criado = service.criar(req);
        return ResponseEntity.created(URI.create("/api/usuarios/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @RequestBody UsuarioUpdateRequest req) {
        return service.atualizar(id, req);
    }
}