package br.edu.upe.sgba.dto;

import br.edu.upe.sgba.entity.enums.StatusSubmissao;

/**
 * Corpo do PATCH /api/submissoes/{id}/status (RF16 pelo gerenciador).
 * RN04: motivo é obrigatório quando novoStatus ∈ {PENDENTE_DOCUMENTOS, INDEFERIDA}.
 */
public record DecisaoRequest(StatusSubmissao novoStatus, String motivo) {
}
