package br.edu.upe.sgba.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.edu.upe.sgba.entity.Edital;
import br.edu.upe.sgba.entity.PerguntaEdital;
import br.edu.upe.sgba.entity.RespostaSubmissao;
import br.edu.upe.sgba.entity.Submissao;
import br.edu.upe.sgba.entity.Usuario;
import br.edu.upe.sgba.entity.enums.StatusSubmissao;

/**
 * Detalhe completo da submissão (GET/{id} e resposta de PUT/PATCH) — inclui as
 * respostas do formulário, o edital resumido (janela RN05) e o snapshot do
 * mérito do envio (RN09: campos *_Momento).
 */
public record SubmissaoDetalheResponse(
        Long id,
        String protocolo,
        StatusSubmissao status,
        String tituloProjeto,
        String projetoResumo,
        String planoTrabalho,
        Boolean termoAceite,
        LocalDateTime dataEnvio,
        LocalDateTime criadoEm,
        EditalResumoResposta edital,
        OrientadorResumoResposta orientador,
        List<RespostaView> respostas,
        Integer qtdDocumentos,
        String cursoMomento,
        Short periodoMomento,
        BigDecimal craMomento,
        String unidadeMomento) {

    record EditalResumoResposta(Long id, String numeroEdital, Short ano,
            LocalDateTime dataInicioSubmissoes, LocalDateTime dataFimSubmissoes, String programaTitulo) {
    }

    record OrientadorResumoResposta(Long id, String nome) {
    }

    record RespostaView(Long perguntaId, String enunciado, Boolean obrigatoria, String respostaTexto) {
    }

    public static SubmissaoDetalheResponse from(Submissao s, List<RespostaSubmissao> respostas, int qtdDocumentos) {
        Edital e = s.getEdital();
        Usuario orientador = s.getOrientador();
        List<RespostaView> respostasView = respostas.stream().map(r -> {
            PerguntaEdital p = r.getPergunta();
            return new RespostaView(p.getId(), p.getEnunciado(), p.getObrigatoria(), r.getRespostaTexto());
        }).toList();
        return new SubmissaoDetalheResponse(
                s.getId(), s.getProtocolo(), s.getStatus(),
                s.getTituloProjeto(), s.getProjetoResumo(), s.getPlanoTrabalho(),
                s.getTermoAceite(), s.getDataEnvio(), s.getCriadoEm(),
                new EditalResumoResposta(e.getId(), e.getNumeroEdital(), e.getAno(),
                        e.getDataInicioSubmissoes(), e.getDataFimSubmissoes(),
                        e.getPrograma() == null ? null : e.getPrograma().getTitulo()),
                orientador == null ? null : new OrientadorResumoResposta(orientador.getId(), orientador.getNome()),
                respostasView, qtdDocumentos,
                s.getCursoMomento(), s.getPeriodoMomento(), s.getCraMomento(), s.getUnidadeMomento());
    }
}
