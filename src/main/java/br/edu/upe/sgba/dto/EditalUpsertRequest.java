package br.edu.upe.sgba.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** programaId é obrigatório no POST e ignorado no PUT. dataPublicacao é opcional (default: hoje). */
public record EditalUpsertRequest(
        Long programaId,
        String numeroEdital,
        Short ano,
        LocalDate dataPublicacao,
        LocalDateTime dataInicioSubmissoes,
        LocalDateTime dataFimSubmissoes) {}