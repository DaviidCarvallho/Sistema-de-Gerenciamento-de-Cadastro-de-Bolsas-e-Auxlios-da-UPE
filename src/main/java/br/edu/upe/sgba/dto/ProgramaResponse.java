package br.edu.upe.sgba.dto;

import br.edu.upe.sgba.entity.ProgramaBolsa;
import br.edu.upe.sgba.entity.enums.CategoriaPrograma;
import br.edu.upe.sgba.entity.enums.StatusPrograma;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Cartão completo do programa (RF03/RF14), incluindo janela e status do enum. */
public record ProgramaResponse(
        Long id,
        String titulo,
        CategoriaPrograma categoria,
        String descricao,
        BigDecimal valorMensal,
        Short vagas,
        String periodoLetivo,
        LocalDateTime dataAbertura,
        LocalDateTime dataLimite,
        StatusPrograma status) {

    public static ProgramaResponse from(ProgramaBolsa p) {
        return new ProgramaResponse(p.getId(), p.getTitulo(), p.getCategoria(), p.getDescricao(),
                p.getValorMensal(), p.getVagas(), p.getPeriodoLetivo(), p.getDataAbertura(),
                p.getDataLimite(), p.getStatus());
    }
}
