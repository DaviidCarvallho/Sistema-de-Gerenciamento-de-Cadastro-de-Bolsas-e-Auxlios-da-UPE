package br.edu.upe.sgba.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.edu.upe.sgba.dto.ArquivoEditalResponse;
import br.edu.upe.sgba.dto.EditalComFormularioResponse;
import br.edu.upe.sgba.dto.EditalResponse;
import br.edu.upe.sgba.dto.EditalUpsertRequest;
import br.edu.upe.sgba.dto.ExigenciaResponse;
import br.edu.upe.sgba.dto.PerguntaResponse;
import br.edu.upe.sgba.entity.Edital;
import br.edu.upe.sgba.entity.ProgramaBolsa;
import br.edu.upe.sgba.exception.RecursoNaoEncontradoException;
import br.edu.upe.sgba.exception.RegraNegocioException;
import br.edu.upe.sgba.repository.EditalRepository;
import br.edu.upe.sgba.repository.ExigenciaEditalRepository;
import br.edu.upe.sgba.repository.PerguntaEditalRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EditalService {

    private final EditalRepository editalRepository;
    private final PerguntaEditalRepository perguntaRepository;
    private final ExigenciaEditalRepository exigenciaRepository;
    private final ArmazenamentoService armazenamento;

    // Busca o programa pelo EntityManager para não acoplar ao repositório da Issue 3.
    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public Page<EditalResponse> listar(Long programaId, Pageable pageable) {
        Page<Edital> pagina = (programaId == null)
                ? editalRepository.findAll(pageable)
                : editalRepository.findByProgramaId(programaId, pageable);
        return pagina.map(EditalResponse::from);
    }

    @Transactional(readOnly = true)
    public EditalResponse buscar(Long id) {
        return EditalResponse.from(buscarEntidade(id));
    }

    /** Perguntas ordenadas por ordemExibicao + exigências (RF06/RF07). */
    @Transactional(readOnly = true)
    public EditalComFormularioResponse formulario(Long id) {
        Edital e = buscarEntidade(id);
        return new EditalComFormularioResponse(
                EditalResponse.from(e),
                perguntaRepository.findByEditalIdOrderByOrdemExibicaoAsc(id).stream()
                        .map(PerguntaResponse::from).toList(),
                exigenciaRepository.findByEditalIdOrderByIdAsc(id).stream()
                        .map(ExigenciaResponse::from).toList());
    }

    @Transactional
    public EditalResponse criar(EditalUpsertRequest req) {
        if (req.programaId() == null) {
            throw Validacao.argumentoInvalido("O campo 'programaId' é obrigatório");
        }
        ProgramaBolsa programa = em.find(ProgramaBolsa.class, req.programaId());
        if (programa == null) {
            throw RecursoNaoEncontradoException.porId("Programa", req.programaId());
        }
        Edital e = new Edital();
        e.setPrograma(programa);
        aplicar(e, req, null);
        return EditalResponse.from(editalRepository.save(e));
    }

    @Transactional
    public EditalResponse atualizar(Long id, EditalUpsertRequest req) {
        Edital e = buscarEntidade(id);
        aplicar(e, req, id);   // programaId é ignorado: o edital não muda de programa
        return EditalResponse.from(e);
    }

    @Transactional
    public ArquivoEditalResponse anexarArquivo(Long id, MultipartFile arquivo) {
        Edital e = buscarEntidade(id);                       // 404 antes de validar o arquivo
        String caminho = armazenamento.salvarPdfEdital(id, arquivo);
        e.setCaminhoArquivoPdf(caminho);
        return new ArquivoEditalResponse(caminho);
    }

    @Transactional(readOnly = true)
    public byte[] baixarArquivo(Long id) {
        Edital e = buscarEntidade(id);
        if (e.getCaminhoArquivoPdf() == null) {
            throw new RecursoNaoEncontradoException("O edital " + id + " ainda não possui arquivo PDF");
        }
        return armazenamento.ler(e.getCaminhoArquivoPdf());
    }

    private Edital buscarEntidade(Long id) {
        return editalRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.porId("Edital", id));
    }

    /** Valida (400 antes de 409) e copia os dados base. idAtual == null → criação. */
    private void aplicar(Edital e, EditalUpsertRequest req, Long idAtual) {
        String numero = Validacao.obrigatorio(req.numeroEdital(), "numeroEdital", 30);
        if (req.ano() == null || req.ano() < 2000 || req.ano() > 2100) {
            throw Validacao.argumentoInvalido("O campo 'ano' deve estar entre 2000 e 2100");
        }
        LocalDateTime inicio = req.dataInicioSubmissoes();
        LocalDateTime fim = req.dataFimSubmissoes();
        if (inicio == null || fim == null) {
            throw Validacao.argumentoInvalido("Informe 'dataInicioSubmissoes' e 'dataFimSubmissoes'");
        }
        if (!fim.isAfter(inicio)) {                                            // RN05-a
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "JANELA_INVALIDA",
                    "A data fim das submissões deve ser posterior à data de início");
        }
        boolean duplicado = (idAtual == null)
                ? editalRepository.existsByNumeroEditalAndAno(numero, req.ano())
                : editalRepository.existsByNumeroEditalAndAnoAndIdNot(numero, req.ano(), idAtual);
        if (duplicado) {
            throw new RegraNegocioException("EDITAL_DUPLICADO",
                    "Já existe edital " + numero + "/" + req.ano());
        }

        e.setNumeroEdital(numero);
        e.setAno(req.ano());
        e.setDataInicioSubmissoes(inicio);
        e.setDataFimSubmissoes(fim);
        if (req.dataPublicacao() != null) {
            e.setDataPublicacao(req.dataPublicacao());
        } else if (e.getDataPublicacao() == null) {
            e.setDataPublicacao(LocalDate.now());
        }
    }
}