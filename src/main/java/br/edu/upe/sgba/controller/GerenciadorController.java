package br.edu.upe.sgba.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.upe.sgba.dto.SubmissaoPainelResponse;
import br.edu.upe.sgba.entity.enums.StatusSubmissao;
import br.edu.upe.sgba.service.SubmissaoService;
import lombok.RequiredArgsConstructor;

/**
 * Painel do gerenciador (RF14) — filtros combináveis por status/edital/programa,
 * paginado (D4). Sem checagem de função no cp3 (§9.2 delimita papel a 2
 * endpoints; esta rota fica para o cp4).
 */
@RestController
@RequestMapping("/api/gerenciador")
@RequiredArgsConstructor
public class GerenciadorController {

    private final SubmissaoService service;

    @GetMapping("/submissoes")
    public Page<SubmissaoPainelResponse> listar(
            @RequestParam(required = false) StatusSubmissao status,
            @RequestParam(required = false) Long editalId,
            @RequestParam(required = false) Long programaId,
            Pageable pageable) {
        return service.listarGerenciador(status, editalId, programaId, pageable);
    }
}
