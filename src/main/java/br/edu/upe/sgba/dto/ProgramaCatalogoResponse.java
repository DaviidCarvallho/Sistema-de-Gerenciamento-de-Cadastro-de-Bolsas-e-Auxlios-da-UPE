package br.edu.upe.sgba.dto;

import br.edu.upe.sgba.entity.ProgramaBolsa;
import br.edu.upe.sgba.entity.enums.CategoriaPrograma;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Card público do catálogo do aluno (RF05) — sem campos administrativos (status). */
public record ProgramaCatalogoResponse(
        Long id,
        String titulo,
        CategoriaPrograma categoria,
        String descricao,
        BigDecimal valorMensal,
        Short vagas,
        String periodoLetivo,
        LocalDateTime dataAbertura,
        LocalDateTime dataLimite) {

    public static ProgramaCatalogoResponse from(ProgramaBolsa p) {
        return new ProgramaCatalogoResponse(p.getId(), p.getTitulo(), p.getCategoria(), p.getDescricao(),
                p.getValorMensal(), p.getVagas(), p.getPeriodoLetivo(), p.getDataAbertura(), p.getDataLimite());
    }
}
