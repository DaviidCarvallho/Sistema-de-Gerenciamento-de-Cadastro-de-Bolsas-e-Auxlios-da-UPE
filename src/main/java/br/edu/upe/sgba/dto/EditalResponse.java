package br.edu.upe.sgba.dto;

import br.edu.upe.sgba.entity.Edital;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EditalResponse(
        Long id, Long programaId, String programaTitulo, String numeroEdital, Short ano,
        LocalDate dataPublicacao, LocalDateTime dataInicioSubmissoes, LocalDateTime dataFimSubmissoes,
        boolean submissoesAbertas, String caminhoArquivoPdf) {

    /** Chamar dentro de transação (acessa o programa, que é LAZY). */
    public static EditalResponse from(Edital e) {
        LocalDateTime agora = LocalDateTime.now();
        boolean abertas = !agora.isBefore(e.getDataInicioSubmissoes())
                && !agora.isAfter(e.getDataFimSubmissoes());
        return new EditalResponse(e.getId(), e.getPrograma().getId(), e.getPrograma().getTitulo(),
                e.getNumeroEdital(), e.getAno(), e.getDataPublicacao(), e.getDataInicioSubmissoes(),
                e.getDataFimSubmissoes(), abertas, e.getCaminhoArquivoPdf());
    }
}