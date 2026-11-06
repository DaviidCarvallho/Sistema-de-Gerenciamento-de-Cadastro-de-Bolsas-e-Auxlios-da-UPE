package br.edu.upe.sgba.dto;

import java.time.LocalDateTime;

import br.edu.upe.sgba.entity.Edital;
import br.edu.upe.sgba.entity.Submissao;
import br.edu.upe.sgba.entity.enums.StatusSubmissao;

/**
 * Card do painel (RF11 do aluno, RF14 do gerenciador): protocolo, edital/programa,
 * status, dataEnvio e qtdDocumentos.
 */
public record SubmissaoPainelResponse(
        Long id,
        String protocolo,
        StatusSubmissao status,
        String tituloProjeto,
        LocalDateTime dataEnvio,
        Long editalId,
        String editalTitulo,
        String programaTitulo,
        Integer qtdDocumentos) {

    public static SubmissaoPainelResponse from(Submissao s, int qtdDocumentos) {
        Edital e = s.getEdital();
        return new SubmissaoPainelResponse(
                s.getId(), s.getProtocolo(), s.getStatus(), s.getTituloProjeto(), s.getDataEnvio(),
                e == null ? null : e.getId(),
                e == null ? null : e.getNumeroEdital() + "/" + e.getAno(),
                e == null || e.getPrograma() == null ? null : e.getPrograma().getTitulo(),
                qtdDocumentos);
    }
}
