package br.edu.upe.sgba.dto;

import java.time.LocalDateTime;

import br.edu.upe.sgba.entity.Submissao;
import br.edu.upe.sgba.entity.enums.StatusSubmissao;

/** Resposta do POST /{id}/enviar (200): protocolo (RN02), status=RECEBIDA e dataEnvio (RN09). */
public record SubmissaoEnviadaResponse(
        Long id,
        String protocolo,
        StatusSubmissao status,
        LocalDateTime dataEnvio) {

    public static SubmissaoEnviadaResponse from(Submissao s) {
        return new SubmissaoEnviadaResponse(s.getId(), s.getProtocolo(), s.getStatus(), s.getDataEnvio());
    }
}
