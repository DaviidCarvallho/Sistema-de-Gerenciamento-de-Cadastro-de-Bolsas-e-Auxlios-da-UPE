package br.edu.upe.sgba.service;

import br.edu.upe.sgba.dto.ProgramaCatalogoResponse;
import br.edu.upe.sgba.dto.ProgramaResponse;
import br.edu.upe.sgba.dto.ProgramaUpsertRequest;
import br.edu.upe.sgba.entity.ProgramaBolsa;
import br.edu.upe.sgba.entity.enums.CategoriaPrograma;
import br.edu.upe.sgba.entity.enums.StatusPrograma;
import br.edu.upe.sgba.exception.RecursoNaoEncontradoException;
import br.edu.upe.sgba.exception.RegraNegocioException;
import br.edu.upe.sgba.repository.ProgramaBolsaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Módulo Programas de Bolsa (RF03/RF05) — Issue 3 (Checkpoint 3).
 *
 * <p>Regras: janela de visibilidade por datas ({@code listarAbertos} usa datas
 * efetivas — agora entre abertura/limite e status ABERTO), validação de período
 * na criação/atualização (400 {@code PERIODO_INVALIDO}) e encerramento manual
 * pelo gerenciador (RF03) sem repetição (409 {@code PROGRAMA_JA_ENCERRADO}).</p>
 *
 * <p>Erros seguem o contrato D2 ({@link RegraNegocioException}/
 * {@link RecursoNaoEncontradoException} → {@code ErrorResponse} no handler base).</p>
 */
@Service
@RequiredArgsConstructor
public class ProgramaBolsaService {

    private final ProgramaBolsaRepository repository;

    @Transactional
    public ProgramaResponse cadastrar(ProgramaUpsertRequest request) {
        validarPeriodo(request.dataAbertura(), request.dataLimite());
        ProgramaBolsa programa = new ProgramaBolsa();
        preencherPrograma(programa, request);
        return ProgramaResponse.from(repository.save(programa));
    }

    @Transactional(readOnly = true)
    public ProgramaResponse buscarPorId(Long id) {
        return ProgramaResponse.from(buscarEntidade(id));
    }

    /** RF05: catálogo público do aluno — apenas programas ABERTOS dentro da janela (datas efetivas). */
    @Transactional(readOnly = true)
    public List<ProgramaCatalogoResponse> listarAbertos() {
        LocalDateTime agora = LocalDateTime.now();
        return repository.findByStatusAndDataAberturaLessThanEqualAndDataLimiteGreaterThanEqual(StatusPrograma.ABERTO, agora, agora)
                .stream().map(ProgramaCatalogoResponse::from).toList();
    }

    /** RF03/RF14: listagem completa para o gerenciador, com filtros combináveis e paginação (D4). */
    @Transactional(readOnly = true)
    public Page<ProgramaResponse> listarComFiltros(CategoriaPrograma categoria, String q, StatusPrograma status, Pageable pageable) {
        Specification<ProgramaBolsa> filtro = (root, query, cb) -> cb.conjunction();

        if (categoria != null) {
            filtro = filtro.and((root, query, cb) -> cb.equal(root.get("categoria"), categoria));
        }
        if (q != null && !q.isBlank()) {
            filtro = filtro.and((root, query, cb) -> cb.like(cb.lower(root.get("titulo")), "%" + q.toLowerCase() + "%"));
        }
        if (status != null) {
            filtro = filtro.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        return repository.findAll(filtro, pageable).map(ProgramaResponse::from);
    }

    @Transactional
    public ProgramaResponse atualizar(Long id, ProgramaUpsertRequest request) {
        ProgramaBolsa programa = buscarEntidade(id);
        validarPeriodo(request.dataAbertura(), request.dataLimite());
        preencherPrograma(programa, request);
        return ProgramaResponse.from(programa);
    }

    /** RF03: transição manual ABERTO/EM_BREVE → ENCERRADO; repetição é conflito (409). */
    @Transactional
    public ProgramaResponse encerrar(Long id) {
        ProgramaBolsa programa = buscarEntidade(id);

        if (programa.getStatus() == StatusPrograma.ENCERRADO) {
            throw new RegraNegocioException("PROGRAMA_JA_ENCERRADO", "Este programa já está encerrado");
        }

        programa.setStatus(StatusPrograma.ENCERRADO);
        return ProgramaResponse.from(programa);
    }

    private ProgramaBolsa buscarEntidade(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.porId("Programa", id));
    }

    private void validarPeriodo(LocalDateTime abertura, LocalDateTime limite) {
        if (abertura == null || limite == null || !limite.isAfter(abertura)) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "PERIODO_INVALIDO",
                    "A data limite deve ser posterior à data de abertura");
        }
    }

    private void preencherPrograma(ProgramaBolsa programa, ProgramaUpsertRequest request) {
        programa.setTitulo(request.titulo());
        programa.setCategoria(request.categoria());
        programa.setDescricao(request.descricao());
        programa.setValorMensal(request.valorMensal());
        programa.setVagas(request.vagas());
        programa.setPeriodoLetivo(request.periodoLetivo());
        programa.setDataAbertura(request.dataAbertura());
        programa.setDataLimite(request.dataLimite());
        programa.setStatus(request.status());
    }
}
