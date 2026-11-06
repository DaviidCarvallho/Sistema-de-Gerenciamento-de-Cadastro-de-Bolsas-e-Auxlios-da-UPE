package br.edu.upe.sgba.dto;

import java.util.List;

/** Ficha do edital + perguntas (ordenadas por ordemExibicao) + exigências. Usado pelo wizard (RF07). */
public record EditalComFormularioResponse(
        EditalResponse edital,
        List<PerguntaResponse> perguntas,
        List<ExigenciaResponse> exigencias) {}