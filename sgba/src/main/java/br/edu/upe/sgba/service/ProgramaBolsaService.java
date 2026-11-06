package br.edu.upe.sgba.service;

import br.edu.upe.sgba.dto.ProgramaUpsertRequest;
import br.edu.upe.sgba.dto.ProgramaResponse;
import br.edu.upe.sgba.dto.ProgramaCatalogoResponse;
import br.edu.upe.sgba.entity.ProgramaBolsa;
import br.edu.upe.sgba.enums.CategoriaPrograma;
import br.edu.upe.sgba.enums.StatusPrograma;
import br.edu.upe.sgba.repository.ProgramaBolsaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProgramaBolsaService {

    private final ProgramaBolsaRepository repository;

    public ProgramaBolsaService(ProgramaBolsaRepository repository) {
        this.repository = repository;
    }

    // REGRA DE DATAS
    private void validarPeriodo(LocalDateTime abertura, LocalDateTime limite) {

        if (abertura == null || limite == null || !limite.isAfter(abertura)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PERIODO_INVALIDO");
        }
    }

    // CADASTRAR
    public ProgramaResponse cadastrar(ProgramaUpsertRequest request) {

        validarPeriodo(request.getDataAbertura(), request.getDataLimite());
        ProgramaBolsa programa = new ProgramaBolsa();
        preencherPrograma(programa, request);
        ProgramaBolsa salvo = repository.save(programa);

        return converterParaResponse(salvo);
    }

    // BUSCAR POR ID
    public ProgramaResponse buscarPorId(Long id) {
        ProgramaBolsa programa = buscarEntidade(id);

        return converterParaResponse(programa);
    }

    private ProgramaBolsa buscarEntidade(Long id) {

        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PROGRAMA_NAO_ENCONTRADO"));
    }

    // ATUALIZAR
    public ProgramaResponse atualizar(Long id, ProgramaUpsertRequest request) {

        ProgramaBolsa programa = buscarEntidade(id);
        validarPeriodo(request.getDataAbertura(), request.getDataLimite());
        preencherPrograma(programa, request);
        ProgramaBolsa salvo = repository.save(programa);

        return converterParaResponse(salvo);
    }

    // ENCERRAR
    public ProgramaResponse encerrar(Long id) {

        ProgramaBolsa programa = buscarEntidade(id);

        if (programa.getStatus() == StatusPrograma.ENCERRADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PROGRAMA_JA_ENCERRADO");
        }

        programa.setStatus(StatusPrograma.ENCERRADO);
        ProgramaBolsa salvo = repository.save(programa);

        return converterParaResponse(salvo);
    }

    // LISTAR ABERTOS
    public List<ProgramaCatalogoResponse> listarAbertos() {
        LocalDateTime agora = LocalDateTime.now();
        List<ProgramaBolsa> programas = repository.findByStatusAndDataAberturaLessThanEqualAndDataLimiteGreaterThanEqual(StatusPrograma.ABERTO, agora, agora);

        return programas.stream().map(this::converterParaCatalogo).toList();
    }

    // FILTROS COMBINADOS E PAGINAÇÃO
    public Page<ProgramaResponse> listarComFiltros(CategoriaPrograma categoria, String q, StatusPrograma status, Pageable pageable) {
        Specification<ProgramaBolsa> filtro = (root, query, cb) -> cb.conjunction();

        if (categoria != null) {
            filtro = filtro.and((root, query, cb) -> cb.equal(root.get("categoria"), categoria));
        }

        if (q != null && !q.isBlank()) {

            String texto = "%" + q.toLowerCase() + "%";

            filtro = filtro.and((root, query, cb) -> cb.like(cb.lower(root.get("titulo")), texto));
        }

        if (status != null) {
            filtro = filtro.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        return repository.findAll(filtro, pageable).map(this::converterParaResponse);
    }

    // PREENCHER ENTIDADE
    private void preencherPrograma(ProgramaBolsa programa, ProgramaUpsertRequest request) {

        programa.setTitulo(request.getTitulo());
        programa.setCategoria(request.getCategoria());
        programa.setDescricao(request.getDescricao());
        programa.setValorMensal(request.getValorMensal());
        programa.setVagas(request.getVagas());
        programa.setPeriodoLetivo(request.getPeriodoLetivo());
        programa.setDataAbertura(request.getDataAbertura());
        programa.setDataLimite(request.getDataLimite());
        programa.setStatus(request.getStatus());
    }

    // CONVERTER PARA RESPONSE
    private ProgramaResponse converterParaResponse(ProgramaBolsa programa) {

        ProgramaResponse response = new ProgramaResponse();

        response.setId(programa.getId());
        response.setTitulo(programa.getTitulo());
        response.setCategoria(programa.getCategoria());
        response.setDescricao(programa.getDescricao());
        response.setValorMensal(programa.getValorMensal());
        response.setVagas(programa.getVagas());
        response.setPeriodoLetivo(programa.getPeriodoLetivo());
        response.setDataAbertura(programa.getDataAbertura());
        response.setDataLimite(programa.getDataLimite());
        response.setStatus(programa.getStatus());

        return response;
    }

    // CONVERTER PARA CATÁLOGO
    private ProgramaCatalogoResponse converterParaCatalogo(ProgramaBolsa programa) {

        ProgramaCatalogoResponse response = new ProgramaCatalogoResponse();

        response.setId(programa.getId());
        response.setTitulo(programa.getTitulo());
        response.setCategoria(programa.getCategoria());
        response.setDescricao(programa.getDescricao());
        response.setValorMensal(programa.getValorMensal());
        response.setVagas(programa.getVagas());
        response.setPeriodoLetivo(programa.getPeriodoLetivo());
        response.setDataAbertura(programa.getDataAbertura());
        response.setDataLimite(programa.getDataLimite());

        return response;
    }
}