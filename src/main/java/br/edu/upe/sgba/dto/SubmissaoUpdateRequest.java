package br.edu.upe.sgba.dto;

import java.util.List;

/**
 * Corpo do PUT /api/submissoes/{id} — atualiza os dados do rascunho + respostas[]
 * (upsert) + termoAceite (RN03: só RASCUNHO). Campos nulos não alteram o valor atual;
 * o edital não muda no PUT (ignorado).
 */
public record SubmissaoUpdateRequest(
        String tituloProjeto,
        String projetoResumo,
        String planoTrabalho,
        Long orientadorId,
        List<RespostaUpsertRequest> respostas,
        Boolean termoAceite) {
}
