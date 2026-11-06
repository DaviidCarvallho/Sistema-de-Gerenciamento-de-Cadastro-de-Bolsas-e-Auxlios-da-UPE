package br.edu.upe.sgba.dto;

/** Corpo do POST /api/submissoes — cria o rascunho (RN08/RN10). */
public record SubmissaoRascunhoRequest(
        Long editalId,
        String tituloProjeto,
        String projetoResumo,
        String planoTrabalho,
        Long orientadorId) {
}
