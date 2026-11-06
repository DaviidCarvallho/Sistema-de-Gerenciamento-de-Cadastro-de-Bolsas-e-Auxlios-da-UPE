package br.edu.upe.sgba.dto;

import br.edu.upe.sgba.entity.PerguntaEdital;

public record PerguntaResponse(Long id, String enunciado, Boolean obrigatoria, Short ordemExibicao) {
    public static PerguntaResponse from(PerguntaEdital p) {
        return new PerguntaResponse(p.getId(), p.getEnunciado(), p.getObrigatoria(), p.getOrdemExibicao());
    }
}