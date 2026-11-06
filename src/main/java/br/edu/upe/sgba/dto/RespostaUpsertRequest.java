package br.edu.upe.sgba.dto;

/** Resposta dela formulário do edital (RF07.3) — upsert por perguntaId (uk_resposta_unica). */
public record RespostaUpsertRequest(Long perguntaId, String respostaTexto) {
}
