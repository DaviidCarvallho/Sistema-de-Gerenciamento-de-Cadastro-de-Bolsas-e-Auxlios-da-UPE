package br.edu.upe.sgba.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.upe.sgba.dto.PerguntaRequest;
import br.edu.upe.sgba.dto.PerguntaResponse;
import br.edu.upe.sgba.entity.Edital;
import br.edu.upe.sgba.entity.PerguntaEdital;
import br.edu.upe.sgba.exception.RecursoNaoEncontradoException;
import br.edu.upe.sgba.exception.RegraNegocioException;
import br.edu.upe.sgba.repository.EditalRepository;
import br.edu.upe.sgba.repository.PerguntaEditalRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PerguntaEditalService {

    private final EditalRepository editalRepository;
    private final PerguntaEditalRepository perguntaRepository;

    @Transactional
    public PerguntaResponse criar(Long editalId, PerguntaRequest req) {
        Edital edital = editalRepository.findById(editalId)
                .orElseThrow(() -> RecursoNaoEncontradoException.porId("Edital", editalId));
        String enunciado = Validacao.obrigatorio(req.enunciado(), "enunciado", 2000);
        Boolean obrigatoria = exigirObrigatoria(req.obrigatoria());
        Short ordem = exigirOrdem(req.ordemExibicao());
        if (perguntaRepository.existsByEditalIdAndOrdemExibicao(editalId, ordem)) {
            throw ordemUsada(ordem);
        }
        PerguntaEdital p = new PerguntaEdital();
        p.setEdital(edital);
        p.setEnunciado(enunciado);
        p.setObrigatoria(obrigatoria);
        p.setOrdemExibicao(ordem);
        return PerguntaResponse.from(perguntaRepository.save(p));
    }

    @Transactional
    public PerguntaResponse atualizar(Long editalId, Long perguntaId, PerguntaRequest req) {
        PerguntaEdital p = buscar(editalId, perguntaId);
        String enunciado = Validacao.obrigatorio(req.enunciado(), "enunciado", 2000);
        Boolean obrigatoria = exigirObrigatoria(req.obrigatoria());
        Short ordem = exigirOrdem(req.ordemExibicao());
        if (perguntaRepository.existsByEditalIdAndOrdemExibicaoAndIdNot(editalId, ordem, perguntaId)) {
            throw ordemUsada(ordem);
        }
        p.setEnunciado(enunciado);
        p.setObrigatoria(obrigatoria);
        p.setOrdemExibicao(ordem);
        return PerguntaResponse.from(p);
    }

    @Transactional
    public void remover(Long editalId, Long perguntaId) {
        perguntaRepository.delete(buscar(editalId, perguntaId));
    }

    private PerguntaEdital buscar(Long editalId, Long perguntaId) {
        if (!editalRepository.existsById(editalId)) {
            throw RecursoNaoEncontradoException.porId("Edital", editalId);
        }
        return perguntaRepository.findByIdAndEditalId(perguntaId, editalId)
                .orElseThrow(() -> RecursoNaoEncontradoException.porId("Pergunta", perguntaId));
    }

    private Boolean exigirObrigatoria(Boolean v) {
        if (v == null) throw Validacao.argumentoInvalido("O campo 'obrigatoria' é obrigatório");
        return v;
    }

    private Short exigirOrdem(Short v) {
        if (v == null || v < 1) {
            throw Validacao.argumentoInvalido("O campo 'ordemExibicao' deve ser um número maior ou igual a 1");
        }
        return v;
    }

    private RegraNegocioException ordemUsada(Short ordem) {
        return new RegraNegocioException("ORDEM_JA_USADA",
                "Já existe pergunta com ordem de exibição " + ordem + " neste edital");
    }
}