package br.edu.upe.sgba.dto;

import java.time.LocalDateTime;

import br.edu.upe.sgba.entity.HistoricoStatus;
import br.edu.upe.sgba.entity.enums.StatusSubmissao;

/** Evento da timeline (RF12), ordenado do mais recente para o mais antigo (RN07). */
public record HistoricoResponse(
        Long id,
        StatusSubmissao statusAnterior,
        StatusSubmissao statusNovo,
        String observacao,
        LocalDateTime dataHora,
        String usuarioNome) {

    public static HistoricoResponse from(HistoricoStatus h) {
        return new HistoricoResponse(h.getId(), h.getStatusAnterior(), h.getStatusNovo(),
                h.getObservacao(), h.getDataHora(), h.getUsuario() == null ? null : h.getUsuario().getNome());
    }
}
