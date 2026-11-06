package br.edu.upe.sgba.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.EnumMap;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.upe.sgba.dto.DecisaoRequest;
import br.edu.upe.sgba.dto.HistoricoResponse;
import br.edu.upe.sgba.dto.SubmissaoDetalheResponse;
import br.edu.upe.sgba.dto.SubmissaoEnviadaResponse;
import br.edu.upe.sgba.dto.SubmissaoPainelResponse;
import br.edu.upe.sgba.dto.SubmissaoRascunhoRequest;
import br.edu.upe.sgba.dto.SubmissaoUpdateRequest;
import br.edu.upe.sgba.entity.Aluno;
import br.edu.upe.sgba.entity.Edital;
import br.edu.upe.sgba.entity.HistoricoStatus;
import br.edu.upe.sgba.entity.PerguntaEdital;
import br.edu.upe.sgba.entity.RespostaSubmissao;
import br.edu.upe.sgba.entity.Submissao;
import br.edu.upe.sgba.entity.Usuario;
import br.edu.upe.sgba.entity.enums.FuncaoUsuario;
import br.edu.upe.sgba.entity.enums.StatusSubmissao;
import br.edu.upe.sgba.exception.RecursoNaoEncontradoException;
import br.edu.upe.sgba.exception.RegraNegocioException;
import br.edu.upe.sgba.repository.AlunoRepository;
import br.edu.upe.sgba.repository.DocumentoAnexoRepository;
import br.edu.upe.sgba.repository.EditalRepository;
import br.edu.upe.sgba.repository.ExigenciaEditalRepository;
import br.edu.upe.sgba.repository.HistoricoStatusRepository;
import br.edu.upe.sgba.repository.PerguntaEditalRepository;
import br.edu.upe.sgba.repository.RespostaSubmissaoRepository;
import br.edu.upe.sgba.repository.SubmissaoRepository;
import br.edu.upe.sgba.service.port.NotificacaoPort;
import lombok.RequiredArgsConstructor;

/**
 * Ciclo de vida da submissão (Issue 5 — módulo central): rascunho (RN08/RN10),
 * edição (RN03), envio (RN02/RN05/RN09/RN12) e histórico append-only (RN07/RF12),
 * transições de status do gerenciador (RF16/RN04/§9.2) e o painel (RF11/RF14).
 *
 * <p>Isolamento D1/RN06: o usuário vem do header {@code X-Usuario-Id}; consulta
 * do recurso aceita dono <b>ou</b> GERENCIADOR, e o restante responde 404 sem
 * vazar existência. Escritas do aluno exigem o dono.</p>
 */
@Service
@RequiredArgsConstructor
public class SubmissaoService {

    private final SubmissaoRepository submissoes;
    private final RespostaSubmissaoRepository respostasRepository;
    private final HistoricoStatusRepository historicosRepository;
    private final DocumentoAnexoRepository documentosRepository;
    private final EditalRepository editaisRepository;
    private final PerguntaEditalRepository perguntasRepository;
    private final ExigenciaEditalRepository exigenciasRepository;
    private final AlunoRepository alunosRepository;
    private final UsuarioService usuarios;
    private final NotificacaoPort notificacoes;

    // ------------------------------ rascunho ------------------------------

    /** POST — cria o rascunho: RN08 (1 por edital por aluno) e RN10 (orientador professor). */
    @Transactional
    public SubmissaoDetalheResponse criar(Long alunoId, SubmissaoRascunhoRequest req) {
        exigirUsuarioId(alunoId);
        if (req.editalId() == null) {
            throw Validacao.argumentoInvalido("O campo 'editalId' é obrigatório");
        }
        Edital edital = editaisRepository.findById(req.editalId())
                .orElseThrow(() -> RecursoNaoEncontradoException.porId("Edital", req.editalId()));
        if (submissoes.existsByAlunoIdAndEditalId(alunoId, edital.getId())) {          // RN08
            throw new RegraNegocioException("SUBMISSAO_DUPLICADA",
                    "Aluno já possui submissão para este edital");
        }
        Usuario orientador = validarOrientador(req.orientadorId());                     // RN10

        Submissao s = new Submissao();
        s.setAluno(usuarios.buscarEntidade(alunoId));
        s.setEdital(edital);
        s.setOrientador(orientador);
        s.setStatus(StatusSubmissao.RASCUNHO);
        s.setTermoAceite(Boolean.FALSE);
        s.setTituloProjeto(Validacao.obrigatorio(req.tituloProjeto(), "tituloProjeto", 200));
        s.setProjetoResumo(Validacao.opcional(req.projetoResumo(), "projetoResumo", 5000));
        s.setPlanoTrabalho(Validacao.opcional(req.planoTrabalho(), "planoTrabalho", 5000));
        Submissao salva = submissoes.save(s);
        return detalhe(salva.getId(), alunoId);
    }

    /** Painel do aluno (RF11) — header D1. */
    @Transactional(readOnly = true)
    public List<SubmissaoPainelResponse> minhas(Long usuarioId) {
        exigirUsuarioId(usuarioId);
        return submissoes.findByAlunoIdOrderByCriadoEmDesc(usuarioId).stream()
                .map(s -> SubmissaoPainelResponse.from(s, documentosRepository.countBySubmissaoId(s.getId())))
                .toList();
    }

    /** GET /{id} — dono OU gerenciador (RN06/§9.2); não dono → 404 sem vazar. */
    @Transactional(readOnly = true)
    public SubmissaoDetalheResponse detalhe(Long id, Long usuarioId) {
        Submissao s = buscar(id);
        assegurarVisibilidade(s, usuarioId);
        return montarDetalhe(s);
    }

    /** PUT — atualiza rascunho (RN03: só RASCUNHO), respostas em upsert e termoAceite. */
    @Transactional
    public SubmissaoDetalheResponse atualizar(Long id, Long usuarioId, SubmissaoUpdateRequest req) {
        Submissao s = buscar(id);
        exigirUsuarioId(usuarioId);
        if (!s.getAluno().getId().equals(usuarioId)) {                                  // escrita = dono
            throw RecursoNaoEncontradoException.porId("Submissao", id);
        }
        if (s.getStatus() != StatusSubmissao.RASCUNHO) {                                // RN03
            throw new RegraNegocioException("STATUS_NAO_EDITAVEL",
                    "A submissão só é editável em RASCUNHO (status atual: " + s.getStatus() + ")");
        }
        if (req.tituloProjeto() != null) {
            s.setTituloProjeto(Validacao.obrigatorio(req.tituloProjeto(), "tituloProjeto", 200));
        }
        if (req.projetoResumo() != null) {
            s.setProjetoResumo(Validacao.opcional(req.projetoResumo(), "projetoResumo", 5000));
        }
        if (req.planoTrabalho() != null) {
            s.setPlanoTrabalho(Validacao.opcional(req.planoTrabalho(), "planoTrabalho", 5000));
        }
        if (req.orientadorId() != null) {
            s.setOrientador(validarOrientador(req.orientadorId()));                     // RN10
        }
        if (req.termoAceite() != null) {
            s.setTermoAceite(req.termoAceite());
        }
        if (req.respostas() != null) {
            upsertRespostas(s, req.respostas());
        }
        return montarDetalhe(s);
    }

    // ============================== envio (RN02/RN05/RN09/RN12) e RN07/RF12 ==============================

    /** POST /{id}/enviar — ação do aluno dono; valida RN05 + RN12 e executa RN02 + RN09 + RN07. */
    @Transactional
    public SubmissaoEnviadaResponse enviar(Long id, Long usuarioId) {
        Submissao s = buscar(id);
        exigirUsuarioId(usuarioId);
        if (!s.getAluno().getId().equals(usuarioId)) {                          // envio = dono (RF07)
            throw RecursoNaoEncontradoException.porId("Submissao", id);
        }
        if (s.getStatus() != StatusSubmissao.RASCUNHO) {                        // máquina RF16: enviar só do RASCUNHO
            throw new RegraNegocioException("TRANSICAO_INVALIDA",
                    "A submissão não está em RASCUNHO (status atual: " + s.getStatus() + ")");
        }
        Edital edital = s.getEdital();
        LocalDateTime agora = LocalDateTime.now();
        if (agora.isBefore(edital.getDataInicioSubmissoes())                    // RN05 — janela de envio
                || agora.isAfter(edital.getDataFimSubmissoes())) {
            throw new RegraNegocioException("FORA_DO_PRAZO",
                    "A janela de submissões do edital " + edital.getNumeroEdital() + "/" + edital.getAno()
                            + " encerrou em " + edital.getDataFimSubmissoes());
        }
        List<String> pendencias = coletarPendenciasDeEnvio(s);                  // RN12
        if (!pendencias.isEmpty()) {
            throw new RegraNegocioException("ENVIO_INCOMPLETO",
                    "Envio com " + pendencias.size() + " pendência(s): " + String.join(" • ", pendencias));
        }
        Aluno perfil = alunosRepository.findById(s.getAluno().getId())          // RN09 — snapshot do mérito
                .orElseThrow(() -> new RegraNegocioException(HttpStatus.BAD_REQUEST, "ALUNO_SEM_PERFIL",
                        "Complete o perfil de aluno antes de enviar"));
        s.setCursoMomento(perfil.getCurso());
        s.setPeriodoMomento(perfil.getPeriodoAtual());
        s.setCraMomento(perfil.getCraAtual());
        s.setUnidadeMomento(perfil.getUnidadeAcademica());

        s.setProtocolo(gerarProtocolo());          // RN02 (D6)
        s.setDataEnvio(agora);
        s.setStatus(StatusSubmissao.RECEBIDA);
        Submissao salva = submissoes.save(s);

        registrarHistorico(salva, StatusSubmissao.RASCUNHO, StatusSubmissao.RECEBIDA,   // RN07
                "Envio realizado pelo aluno", s.getAluno());
        notificacoes.notificarTransicao(s.getAluno().getId(), s.getId(),        // hook (Issue 6 conecta)
                "Submissão " + s.getProtocolo() + ": RASCUNHO → RECEBIDA");

        return SubmissaoEnviadaResponse.from(salva);
    }

    /** GET /{id}/historico — timeline append-only (RF12); dono OU gerenciador (RN06). */
    @Transactional(readOnly = true)
    public List<HistoricoResponse> historico(Long id, Long usuarioId) {
        Submissao s = buscar(id);
        assegurarVisibilidade(s, usuarioId);
        return historicosRepository.findBySubmissaoIdOrderByDataHoraDesc(id).stream()
                .map(HistoricoResponse::from)
                .toList();
    }

    // ------------------------------ helpers de envio ------------------------------

    /**
     * RN12 — pendências do envio: termoAceite, perguntas obrigatórias respondidas
     * e exigências obrigatórias com documento anexado.
     */
    private List<String> coletarPendenciasDeEnvio(Submissao s) {
        List<String> pendencias = new ArrayList<>();
        if (!Boolean.TRUE.equals(s.getTermoAceite())) {
            pendencias.add("Termo de aceite não aceite");
        }
        Map<Long, String> respostaPorPergunta = respostasRepository.findBySubmissaoId(s.getId()).stream()
                .collect(Collectors.toMap(r -> r.getPergunta().getId(),
                        RespostaSubmissao::getRespostaTexto, (a, b) -> a));
        for (PerguntaEdital pergunta : perguntasRepository
                .findByEditalIdOrderByOrdemExibicaoAsc(s.getEdital().getId())) {
            if (!Boolean.TRUE.equals(pergunta.getObrigatoria())) {
                continue;
            }
            String texto = respostaPorPergunta.get(pergunta.getId());
            if (texto == null || texto.isBlank()) {
                pendencias.add("Pergunta obrigatória não respondida: '" + pergunta.getEnunciado() + "'");
            }
        }
        for (br.edu.upe.sgba.entity.ExigenciaEdital exigencia : exigenciasRepository
                .findByEditalIdOrderByIdAsc(s.getEdital().getId())) {
            if (!Boolean.TRUE.equals(exigencia.getObrigatorio())) {
                continue;
            }
            if (!documentosRepository.existsBySubmissaoIdAndTipoDocumento(
                    s.getId(), exigencia.getTipoDocumento())) {
                pendencias.add("Documento obrigatório não anexado: " + exigencia.getTipoDocumento());
            }
        }
        return pendencias;
    }

    /** RN02 (D6) — protocolo #AAAA-XXXX anual: contador do ano + exists-check, máx. 5 tentativas. */
    private String gerarProtocolo() {
        String prefixo = "#" + LocalDateTime.now().getYear() + "-";
        long base = submissoes.countByProtocoloStartingWith(prefixo);
        for (int tentativa = 1; tentativa <= 5; tentativa++) {
            String candidato = prefixo + String.format("%04d", base + tentativa);
            if (!submissoes.existsByProtocolo(candidato)) {
                return candidato;
            }
        }
        throw new RegraNegocioException("PROTOCOLO_INDISPONIVEL",
                "Não foi possível gerar o protocolo após 5 tentativas");
    }

    /** RN07 — evento de mudança de status na MESMA transação do chamador (append-only). */
    private void registrarHistorico(Submissao s, StatusSubmissao statusAnterior,
            StatusSubmissao statusNovo, String observacao, Usuario usuario) {
        br.edu.upe.sgba.entity.HistoricoStatus evento = new br.edu.upe.sgba.entity.HistoricoStatus();
        evento.setSubmissao(s);
        evento.setStatusAnterior(statusAnterior);
        evento.setStatusNovo(statusNovo);
        evento.setObservacao(observacao);
        evento.setDataHora(LocalDateTime.now());
        evento.setUsuario(usuario);
        historicosRepository.save(evento);
    }

    // ============================== RF16/RN04/§9.2 — status pelo gerenciador ==============================

    /** Tabela RF16 — únicas transições válidas via PATCH (RASCUNHO sai só pelo /enviar). */
    private static final Map<StatusSubmissao, Set<StatusSubmissao>> TRANSICOES_PATCH = new EnumMap<>(
            Map.of(StatusSubmissao.RECEBIDA, Set.of(StatusSubmissao.EM_ANALISE),
                    StatusSubmissao.EM_ANALISE, Set.of(StatusSubmissao.APROVADA,
                            StatusSubmissao.INDEFERIDA, StatusSubmissao.PENDENTE_DOCUMENTOS),
                    StatusSubmissao.PENDENTE_DOCUMENTOS, Set.of(StatusSubmissao.EM_ANALISE,
                            StatusSubmissao.APROVADA, StatusSubmissao.INDEFERIDA),
                    StatusSubmissao.RASCUNHO, Set.of(),
                    StatusSubmissao.APROVADA, Set.of(),
                    StatusSubmissao.INDEFERIDA, Set.of()));

    /** PATCH /{id}/status — exige GERENCIADOR (§9.2) + tabela RF16 + RN04. Ordem: completude do
     *  argumento (motivo, RN04) antes da validade da transição — o roteiro do checkpoint prova
     *  os dois bloqueios independentemente do status corrente. */
    @Transactional
    public SubmissaoDetalheResponse mudarStatus(Long id, Long gerenciadorId, DecisaoRequest req) {
        Submissao s = buscar(id);
        Usuario gerenciador = usuarios.exigeFuncao(gerenciadorId, FuncaoUsuario.GERENCIADOR); // 400/404 §9.2
        if (req == null || req.novoStatus() == null) {
            throw Validacao.argumentoInvalido("Informe 'novoStatus'");
        }
        if ((req.novoStatus() == StatusSubmissao.PENDENTE_DOCUMENTOS
                || req.novoStatus() == StatusSubmissao.INDEFERIDA)
                && (req.motivo() == null || req.motivo().isBlank())) {                           // RN04
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "MOTIVO_OBRIGATORIO",
                    "O motivo é obrigatório para os status PENDENTE_DOCUMENTOS e INDEFERIDA");
        }
        StatusSubmissao anterior = s.getStatus();
        Set<StatusSubmissao> permitidos = TRANSICOES_PATCH.getOrDefault(anterior, Set.of());
        if (!permitidos.contains(req.novoStatus()) || anterior.equals(req.novoStatus())) {       // RF16
            throw new RegraNegocioException("TRANSICAO_INVALIDA",
                    "Transição '" + anterior + "' → '" + req.novoStatus() + "' não é permitida (RF16)");
        }

        s.setStatus(req.novoStatus());
        Submissao salva = submissoes.save(s);
        registrarHistorico(salva, anterior, req.novoStatus(), req.motivo(), gerenciador);        // RN07
        notificacoes.notificarTransicao(s.getAluno().getId(), s.getId(),                         // hook RF17
                "Submissão " + s.getProtocolo() + ": " + anterior + " → " + req.novoStatus()
                        + (req.motivo() != null ? " — Motivo: " + req.motivo() : ""));
        return montarDetalhe(salva);
    }

    /** RF14 — painel do gerenciador com filtros combináveis (D4). */
    @Transactional(readOnly = true)
    public Page<SubmissaoPainelResponse> listarGerenciador(StatusSubmissao status, Long editalId,
            Long programaId, Pageable pageable) {
        return submissoes.filtrar(status, editalId, programaId, pageable)
                .map(s -> SubmissaoPainelResponse.from(s, documentosRepository.countBySubmissaoId(s.getId())));
    }

    // ============================== helpers ==============================

    /** RN10 — orientador informado deve ser usuário existente com funcao=PROFESSOR. */
    private Usuario validarOrientador(Long orientadorId) {
        if (orientadorId == null) {
            return null;
        }
        Usuario orientador = usuarios.buscarEntidade(orientadorId);
        if (orientador.getFuncao() != FuncaoUsuario.PROFESSOR) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "ORIENTADOR_INVALIDO",
                    "O orientador precisa ser um usuário com função PROFESSOR");
        }
        return orientador;
    }

    /** Upsert de respostas por perguntaId (uk_resposta_unica); pergunta deve pertencer ao edital. */
    private void upsertRespostas(Submissao s, List<br.edu.upe.sgba.dto.RespostaUpsertRequest> respostas) {
        for (var r : respostas) {
            if (r.perguntaId() == null) {
                throw Validacao.argumentoInvalido("Cada resposta precisa de 'perguntaId'");
            }
            PerguntaEdital pergunta = perguntasRepository.findById(r.perguntaId())
                    .orElseThrow(() -> RecursoNaoEncontradoException.porId("Pergunta", r.perguntaId()));
            if (!s.getEdital().getId().equals(pergunta.getEdital().getId())) {
                throw RecursoNaoEncontradoException.porId("Pergunta para o edital da submissão",
                        r.perguntaId());
            }
            RespostaSubmissao resposta = respostasRepository
                    .findBySubmissaoIdAndPerguntaId(s.getId(), pergunta.getId())
                    .orElseGet(() -> {
                        RespostaSubmissao nova = new RespostaSubmissao();
                        nova.setSubmissao(s);
                        nova.setPergunta(pergunta);
                        return nova;
                    });
            resposta.setRespostaTexto(Validacao.obrigatorio(r.respostaTexto(), "respostaTexto", 5000));
            s.getRespostas().add(resposta);
            respostasRepository.save(resposta);
        }
    }

    private SubmissaoDetalheResponse montarDetalhe(Submissao s) {
        return SubmissaoDetalheResponse.from(s,
                respostasRepository.findBySubmissaoId(s.getId()),
                documentosRepository.countBySubmissaoId(s.getId()));
    }

    Submissao buscar(Long id) {
        return submissoes.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.porId("Submissao", id));
    }

    /** Header ausente/nulo → 400 (D1: o Service valida o usuário corrente). */
    void exigirUsuarioId(Long usuarioId) {
        if (usuarioId == null) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "ARGUMENTO_INVALIDO",
                    "Usuário não informado (header X-Usuario-Id)");
        }
    }

    /**
     * RN06 (consulta): dono OU função GERENCIADOR vê o recurso; qualquer outro
     * caso → 404 — não vazar existência (decisão registrada no plano/PR).
     */
    void assegurarVisibilidade(Submissao s, Long usuarioId) {
        exigirUsuarioId(usuarioId);
        if (s.getAluno().getId().equals(usuarioId)) {
            return;
        }
        Usuario usuario = usuarios.buscarEntidade(usuarioId);
        if (usuario.getFuncao() == FuncaoUsuario.GERENCIADOR) {
            return;
        }
        throw RecursoNaoEncontradoException.porId("Submissao", s.getId());
    }
}
