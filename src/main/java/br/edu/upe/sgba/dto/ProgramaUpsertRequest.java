package br.edu.upe.sgba.dto;

import br.edu.upe.sgba.entity.enums.CategoriaPrograma;
import br.edu.upe.sgba.entity.enums.StatusPrograma;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Criação/atualização de Programa de Bolsa (RF03). Atualização é completa
 * (contrato do módulo — sem campos nulos recusados) e a janela é validada no Service.
 */
public record ProgramaUpsertRequest(
        String titulo,
        CategoriaPrograma categoria,
        String descricao,
        BigDecimal valorMensal,
        Short vagas,
        String periodoLetivo,
        LocalDateTime dataAbertura,
        LocalDateTime dataLimite,
        StatusPrograma status) {
}
