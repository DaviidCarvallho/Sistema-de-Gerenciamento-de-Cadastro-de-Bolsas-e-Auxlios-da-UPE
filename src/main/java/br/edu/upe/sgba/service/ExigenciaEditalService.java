package br.edu.upe.sgba.service;

import br.edu.upe.sgba.dto.ExigenciaRequest;
import br.edu.upe.sgba.dto.ExigenciaResponse;
import br.edu.upe.sgba.entity.Edital;
import br.edu.upe.sgba.entity.ExigenciaEdital;
import br.edu.upe.sgba.entity.enums.TipoDocumento;
import br.edu.upe.sgba.exception.RecursoNaoEncontradoException;
import br.edu.upe.sgba.exception.RegraNegocioException;
import br.edu.upe.sgba.repository.EditalRepository;
import br.edu.upe.sgba.repository.ExigenciaEditalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExigenciaEditalService {

    private final EditalRepository editalRepository;
    private final ExigenciaEditalRepository exigenciaRepository;

    @Transactional
    public ExigenciaResponse criar(Long editalId, ExigenciaRequest req) {
        Edital edital = editalRepository.findById(editalId)
                .orElseThrow(() -> RecursoNaoEncontradoException.porId("Edital", editalId));
        TipoDocumento tipo = exigirTipo(req.tipoDocumento());
        Boolean obrigatorio = exigirObrigatorio(req.obrigatorio());
        if (exigenciaRepository.existsByEditalIdAndTipoDocumento(editalId, tipo)) {
            throw tipoJaExigido(tipo);
        }
        ExigenciaEdital x = new ExigenciaEdital();
        x.setEdital(edital);
        x.setTipoDocumento(tipo);
        x.setObrigatorio(obrigatorio);
        return ExigenciaResponse.from(exigenciaRepository.save(x));
    }

    @Transactional
    public ExigenciaResponse atualizar(Long editalId, Long exigenciaId, ExigenciaRequest req) {
        ExigenciaEdital x = buscar(editalId, exigenciaId);
        TipoDocumento tipo = exigirTipo(req.tipoDocumento());
        Boolean obrigatorio = exigirObrigatorio(req.obrigatorio());
        if (exigenciaRepository.existsByEditalIdAndTipoDocumentoAndIdNot(editalId, tipo, exigenciaId)) {
            throw tipoJaExigido(tipo);
        }
        x.setTipoDocumento(tipo);
        x.setObrigatorio(obrigatorio);
        return ExigenciaResponse.from(x);
    }

    @Transactional
    public void remover(Long editalId, Long exigenciaId) {
        exigenciaRepository.delete(buscar(editalId, exigenciaId));
    }

    private ExigenciaEdital buscar(Long editalId, Long exigenciaId) {
        if (!editalRepository.existsById(editalId)) {
            throw RecursoNaoEncontradoException.porId("Edital", editalId);
        }
        return exigenciaRepository.findByIdAndEditalId(exigenciaId, editalId)
                .orElseThrow(() -> RecursoNaoEncontradoException.porId("Exigencia", exigenciaId));
    }

    private TipoDocumento exigirTipo(TipoDocumento v) {
        if (v == null) throw Validacao.argumentoInvalido("O campo 'tipoDocumento' é obrigatório");
        return v;
    }

    private Boolean exigirObrigatorio(Boolean v) {
        if (v == null) throw Validacao.argumentoInvalido("O campo 'obrigatorio' é obrigatório");
        return v;
    }

    private RegraNegocioException tipoJaExigido(TipoDocumento tipo) {
        return new RegraNegocioException("TIPO_JA_EXIGIDO",
                "O tipo " + tipo + " já é exigido neste edital");
    }
}