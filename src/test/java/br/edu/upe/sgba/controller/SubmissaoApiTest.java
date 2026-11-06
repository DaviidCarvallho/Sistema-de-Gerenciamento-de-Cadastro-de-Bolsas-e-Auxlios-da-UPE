package br.edu.upe.sgba.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.edu.upe.sgba.entity.Edital;
import br.edu.upe.sgba.entity.PerguntaEdital;
import br.edu.upe.sgba.entity.ProgramaBolsa;
import br.edu.upe.sgba.entity.enums.CategoriaPrograma;
import br.edu.upe.sgba.entity.enums.StatusPrograma;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fluxo da Issue 5 (fundação do módulo central): rascunho — RN08 (duplicada),
 * RN10 (orientador), RN03 (editável), RN06 (isolamento), D1 (header),
 * upsert de respostas por uk_resposta_unica.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SubmissaoApiTest {

    @LocalServerPort
    private int porta;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager tm;

    private RestTestClient rest;

    @BeforeEach
    void setUp() {
        rest = RestTestClient.bindToServer().baseUrl("http://localhost:" + porta).build();
    }

    // ------------------------------ helpers ------------------------------

    private static String sufixoUnico() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private static long extrairId(String corpo) {
        Matcher m = Pattern.compile("\"id\":(\\d+)").matcher(corpo);
        assertTrue(m.find());
        return Long.parseLong(m.group(1));
    }

    private Long criarPrograma() {
        return new TransactionTemplate(tm).execute(status -> {
            ProgramaBolsa p = new ProgramaBolsa();
            p.setTitulo("Programa de Teste");
            p.setCategoria(CategoriaPrograma.INICIACAO_CIENTIFICA);
            p.setValorMensal(new BigDecimal("500.00"));
            p.setVagas((short) 10);
            p.setPeriodoLetivo("2026.2");
            p.setDataAbertura(LocalDateTime.now().minusDays(1));
            p.setDataLimite(LocalDateTime.now().plusDays(30));
            p.setStatus(StatusPrograma.ABERTO);
            em.persist(p);
            return p.getId();
        });
    }

    private Long criarEdital(Long programaId, LocalDateTime inicio, LocalDateTime fim) {
        String corpo = rest.post().uri("/api/editais")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"programaId\":" + programaId + ",\"numeroEdital\":\"T" + sufixoUnico()
                        + "\",\"ano\":2026,\"dataInicioSubmissoes\":\"" + inicio
                        + "\",\"dataFimSubmissoes\":\"" + fim + "\"}")
                .exchange()
                .expectStatus().isEqualTo(201)
                .returnResult(String.class).getResponseBody();
        return extrairId(corpo);
    }

    private Long criarEditalAberto(Long programaId) {
        return criarEdital(programaId, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));
    }

    private Long criarPergunta(Long editalId, boolean obrigatoria) {
        return new TransactionTemplate(tm).execute(status -> {
            PerguntaEdital p = new PerguntaEdital();
            p.setEdital(editalRepositoryBusca(editalId));
            p.setEnunciado("Pergunta " + sufixoUnico() + "?");
            p.setObrigatoria(obrigatoria);
            p.setOrdemExibicao((short) 1);
            em.persist(p);
            return p.getId();
        });
    }

    private Edital editalRepositoryBusca(Long editalId) {
        return em.find(Edital.class, editalId);
    }

    private record AlunoDeTeste(Long usuarioId, Long alunoUsuarioId, Long professorId) {
    }

    /** Usuário ALUNO + perfil completo (para RN09 depois) — via API das Issues 2. */
    private Long criarAluno() {
        String usuarioCorpo = rest.post().uri("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"nome\":\"Aluno Submissao\",\"email\":\"al." + sufixoUnico() + "@upe.br\",\"senha\":\"x\",\"funcao\":\"ALUNO\"}")
                .exchange()
                .expectStatus().isEqualTo(201)
                .returnResult(String.class).getResponseBody();
        Long usuarioId = extrairId(usuarioCorpo);

        String numeros = String.format("%010d",
                Math.abs(java.util.concurrent.ThreadLocalRandom.current().nextLong() % 10_000_000_000L)); // 10 dígitos aleatórios → CPF "1"+numeros = 11 dígitos
        rest.post().uri("/api/alunos")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"usuarioId\":" + usuarioId + ",\"cpf\":\"1" + numeros + "\",\"matricula\":\"2" + numeros
                        + "\",\"craAtual\":8.5,\"curso\":\"Sistemas de Informacao\",\"periodoAtual\":6,"
                        + "\"unidadeAcademica\":\"UPE Campus\"}")
                .exchange()
                .expectStatus().isEqualTo(201);
        return usuarioId;
    }

    private Long criarProfessor(String emailPrefixo) {
        String corpo = rest.post().uri("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"nome\":\"Professor Teste\",\"email\":\"" + emailPrefixo + "@upe.br\",\"senha\":\"x\",\"funcao\":\"PROFESSOR\"}")
                .exchange()
                .expectStatus().isEqualTo(201)
                .returnResult(String.class).getResponseBody();
        return extrairId(corpo);
    }

    private Long criarGerenciador() {
        String corpo = rest.post().uri("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"nome\":\"Gerente Teste\",\"email\":\"gr." + sufixoUnico()
                        + "@upe.br\",\"senha\":\"x\",\"funcao\":\"GERENCIADOR\"}")
                .exchange()
                .expectStatus().isEqualTo(201)
                .returnResult(String.class).getResponseBody();
        return extrairId(corpo);
    }

    private String corpoRascunho(Long editalId) {
        return "{\"editalId\":" + editalId + ",\"tituloProjeto\":\"Projeto " + sufixoUnico()
                + "\",\"projetoResumo\":\"Resumo do projeto\",\"planoTrabalho\":\"Plano\"}";
    }

    // ------------------------------ rascunho ------------------------------

    @Test
    void criarRascunho_valido_retorna201ComLocation() {
        Long alunoId = criarAluno();
        Long editalId = criarEditalAberto(criarPrograma());

        rest.post().uri("/api/submissoes")
                .header("X-Usuario-Id", alunoId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpoRascunho(editalId))
                .exchange()
                .expectStatus().isEqualTo(201)
                .expectHeader().exists("Location")
                .expectBody()
                .jsonPath("$.status").isEqualTo("RASCUNHO")
                .jsonPath("$.protocolo").isEmpty()
                .jsonPath("$.termoAceite").isEqualTo(false);
    }

    @Test
    void criarRascunho_duplicado_retorna409() {
        Long alunoId = criarAluno();
        Long editalId = criarEditalAberto(criarPrograma());

        rest.post().uri("/api/submissoes")
                .header("X-Usuario-Id", alunoId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpoRascunho(editalId))
                .exchange()
                .expectStatus().isEqualTo(201);

        rest.post().uri("/api/submissoes")
                .header("X-Usuario-Id", alunoId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpoRascunho(editalId))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.erro").isEqualTo("SUBMISSAO_DUPLICADA");
    }

    @Test
    void criarRascunho_orientadorDoTipoAluno_retorna400() {
        Long alunoId = criarAluno();
        Long editalId = criarEditalAberto(criarPrograma());

        rest.post().uri("/api/submissoes")
                .header("X-Usuario-Id", alunoId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpoRascunho(editalId) .replace("}", ",\"orientadorId\":" + alunoId + "}"))
                .exchange()
                .expectStatus().isEqualTo(400)
                .expectBody()
                .jsonPath("$.erro").isEqualTo("ORIENTADOR_INVALIDO");
    }

    @Test
    void criarRascunho_semHeader_retorna400() {
        Long editalId = criarEditalAberto(criarPrograma());

        rest.post().uri("/api/submissoes")
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpoRascunho(editalId))
                .exchange()
                .expectStatus().isEqualTo(400)
                .expectBody()
                .jsonPath("$.erro").isEqualTo("ARGUMENTO_INVALIDO");
    }

    @Test
    void atualizarRascunho_respostasUpsertNaoDuplica() {
        Long alunoId = criarAluno();
        Long editalId = criarEditalAberto(criarPrograma());
        Long perguntaId = criarPergunta(editalId, true);
        Long submissaoId = submeter(alunoId, editalId);

        putRespostas(alunoId, submissaoId, perguntaId, "Primeira resposta", true);

        rest.put().uri("/api/submissoes/" + submissaoId)
                .header("X-Usuario-Id", alunoId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"respostas\":[{\"perguntaId\":" + perguntaId
                        + ",\"respostaTexto\":\"Resposta revisada\"}],\"termoAceite\":true}")
                .exchange()
                .expectStatus().isEqualTo(200)
                .expectBody()
                .jsonPath("$.respostas.size()").isEqualTo(1)
                .jsonPath("$.respostas[0].respostaTexto").isEqualTo("Resposta revisada")
                .jsonPath("$.termoAceite").isEqualTo(true);
    }

    @Test
    void atualizarRascunho_respostaDeOutroEdital_retorna404() {
        Long alunoId = criarAluno();
        Long programaId = criarPrograma();
        Long editalId = criarEditalAberto(programaId);
        Long outroEditalId = criarEditalAberto(programaId);
        Long perguntaOutroEdital = criarPergunta(outroEditalId, false);
        Long submissaoId = submeter(alunoId, editalId);

        rest.put().uri("/api/submissoes/" + submissaoId)
                .header("X-Usuario-Id", alunoId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"respostas\":[{\"perguntaId\":" + perguntaOutroEdital
                        + ",\"respostaTexto\":\"tchau\"}]}")
                .exchange()
                .expectStatus().isEqualTo(404);
    }

    // ------------------------------ infra do teste ------------------------------

    private Long submeter(Long alunoId, Long editalId) {
        String corpo = rest.post().uri("/api/submissoes")
                .header("X-Usuario-Id", alunoId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpoRascunho(editalId))
                .exchange()
                .expectStatus().isEqualTo(201)
                .returnResult(String.class).getResponseBody();
        return extrairId(corpo);
    }

    private void putRespostas(Long alunoId, Long submissaoId, Long perguntaId, String texto, boolean termo) {
        rest.put().uri("/api/submissoes/" + submissaoId)
                .header("X-Usuario-Id", alunoId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"respostas\":[{\"perguntaId\":" + perguntaId + ",\"respostaTexto\":\"" + texto
                        + "\"}],\"termoAceite\":" + termo + "}")
                .exchange()
                .expectStatus().isEqualTo(200);
    }

    // ============================== T5 — envio (RN02/RN05/RN09/RN12/RN07) ==============================

    @Test
    void enviar_semRespostaObrigatoria_retorna409ComPendencias() {
        Long alunoId = criarAluno();
        Long editalId = criarEditalAberto(criarPrograma());
        criarPergunta(editalId, true);
        Long submissaoId = putTermoAceite(alunoId, submeter(alunoId, editalId));

        String corpo = postEnviar(alunoId, submissaoId, 409);
        assertTrue(corpo.contains("ENVIO_INCOMPLETO"), "esperava ENVIO_INCOMPLETO, veio: " + corpo);
        assertTrue(corpo.contains("Pergunta obrigatória não respondida"), "esperava pendência do enunciado: " + corpo);
    }

    @Test
    void enviar_semTermoAceite_retorna409() {
        Long alunoId = criarAluno();
        Long editalId = criarEditalAberto(criarPrograma());
        Long perguntaId = criarPergunta(editalId, true);
        Long submissaoId = submeter(alunoId, editalId);

        putRespostas(alunoId, submissaoId, perguntaId, "Resposta completa", false);

        String corpo = postEnviar(alunoId, submissaoId, 409);
        assertTrue(corpo.contains("ENVIO_INCOMPLETO"), "esperava ENVIO_INCOMPLETO, veio: " + corpo);
        assertTrue(corpo.contains("Termo de aceite não aceite"), "esperava pendência do termo: " + corpo);
    }

    @Test
    void enviar_foraDoPrazo_retorna409() {
        Long alunoId = criarAluno();
        Long editalEncerradoId = criarEdital(criarPrograma(),
                LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(1));
        Long submissaoId = submeter(alunoId, editalEncerradoId);

        String corpo = postEnviar(alunoId, submissaoId, 409);
        assertTrue(corpo.contains("FORA_DO_PRAZO"), "esperava FORA_DO_PRAZO, veio: " + corpo);
    }

    @Test
    void enviar_valido_geraProtocoloSnapshotEHistorico() {
        Long alunoId = criarAluno();
        Long editalId = criarEditalAberto(criarPrograma());
        Long perguntaId = criarPergunta(editalId, true);
        Long submissaoId = submeter(alunoId, editalId);
        putRespostas(alunoId, submissaoId, perguntaId, "Resposta completa", true);

        String corpo = postEnviar(alunoId, submissaoId, 200);
        Matcher protocolo = Pattern.compile("#\\d{4}-\\d{4}").matcher(corpo);
        assertTrue(protocolo.find(), "protocolo no formato #AAAA-XXXX: " + corpo);
        assertTrue(corpo.contains("\"status\":\"RECEBIDA\""), "status RECEBIDA: " + corpo);

        // RN09 — snapshot do mérito no envio (campos *_Momento)
        rest.get().uri("/api/submissoes/" + submissaoId)
                .header("X-Usuario-Id", alunoId.toString())
                .exchange()
                .expectStatus().isEqualTo(200)
                .expectBody()
                .jsonPath("$.cursoMomento").isEqualTo("Sistemas de Informacao")
                .jsonPath("$.periodoMomento").isEqualTo(6)
                .jsonPath("$.craMomento").isEqualTo(8.5)
                .jsonPath("$.unidadeMomento").isEqualTo("UPE Campus");

        // RN07/RF12 — 1 evento: RASCUNHO → RECEBIDA
        rest.get().uri("/api/submissoes/" + submissaoId + "/historico")
                .header("X-Usuario-Id", alunoId.toString())
                .exchange()
                .expectStatus().isEqualTo(200)
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].statusAnterior").isEqualTo("RASCUNHO")
                .jsonPath("$[0].statusNovo").isEqualTo("RECEBIDA");
    }

    @Test
    void enviar_protocoloComContadorUnicoParaOutraSubmissao() {
        Long alunoId = criarAluno();
        Long programaId = criarPrograma();
        Long edital1 = criarEditalAberto(programaId);
        Long edital2 = criarEditalAberto(programaId);
        Long sub1 = putTermoAceite(alunoId, submeter(alunoId, edital1));
        Long sub2 = putTermoAceite(alunoId, submeter(alunoId, edital2));

        String corpo1 = postEnviar(alunoId, sub1, 200);
        String corpo2 = postEnviar(alunoId, sub2, 200);
        String p1 = extrairProtocolo(corpo1);
        String p2 = extrairProtocolo(corpo2);
        assertTrue(!p1.equals(p2), "protocolos distintos no mesmo ano: " + p1 + " vs " + p2);
    }

    @Test
    void enviar_porNaoDono_retorna404() {
        Long alunoId = criarAluno();
        Long intrusoId = criarAluno();
        Long editalId = criarEditalAberto(criarPrograma());
        Long submissaoId = submeter(alunoId, editalId);

        rest.post().uri("/api/submissoes/" + submissaoId + "/enviar")
                .header("X-Usuario-Id", intrusoId.toString())
                .exchange()
                .expectStatus().isEqualTo(404);
    }

    @Test
    void enviar_semPerfilDeAluno_retorna400() {
        // usuário ALUNO SEM perfil (sem POST /api/alunos) — RN09 não tem mérito a copiar
        String usuarioCorpo = rest.post().uri("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"nome\":\"Aluno Sem Perfil\",\"email\":\"al.sem." + sufixoUnico()
                        + "@upe.br\",\"senha\":\"x\",\"funcao\":\"ALUNO\"}")
                .exchange()
                .expectStatus().isEqualTo(201)
                .returnResult(String.class).getResponseBody();
        Long alunoId = extrairId(usuarioCorpo);

        Long editalId = criarEditalAberto(criarPrograma()); // sem perguntas/exigências → RN12 ok
        Long submissaoId = putTermoAceite(alunoId, submeter(alunoId, editalId));

        String corpo = postEnviar(alunoId, submissaoId, 400);
        assertTrue(corpo.contains("ALUNO_SEM_PERFIL"), "esperava ALUNO_SEM_PERFIL, veio: " + corpo);
    }

    // --------- helpers de envio ---------

    private Long putTermoAceite(Long alunoId, Long submissaoId) {
        rest.put().uri("/api/submissoes/" + submissaoId)
                .header("X-Usuario-Id", alunoId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"termoAceite\":true}")
                .exchange()
                .expectStatus().isEqualTo(200);
        return submissaoId;
    }

    private String postEnviar(Long usuarioId, Long submissaoId, int statusEsperado) {
        var resposta = rest.post().uri("/api/submissoes/" + submissaoId + "/enviar")
                .header("X-Usuario-Id", String.valueOf(usuarioId))
                .exchange()
                .returnResult(String.class);
        assertTrue(resposta.getStatus().value() == statusEsperado,
                "esperava " + statusEsperado + " mas veio " + resposta.getStatus()
                        + " | corpo: " + resposta.getResponseBody());
        return resposta.getResponseBody();
    }

    private String extrairProtocolo(String corpo) {
        Matcher m = Pattern.compile("#\\d{4}-\\d{4}").matcher(corpo);
        assertTrue(m.find(), "protocolo no corpo: " + corpo);
        return m.group();
    }

    // ============================== T6 — RF16 (PATCH status) + RN04 + §9.2 + RF14 ==============================

    /** Helper do fluxo do gerenciador: PATCH /api/submissoes/{id}/status — asserção de STATUS + corpo. */
    private String patchStatus(Long usuarioId, Long submissaoId, String body, int statusEsperado) {
        var resposta = rest.patch().uri("/api/submissoes/" + submissaoId + "/status")
                .header("X-Usuario-Id", String.valueOf(usuarioId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange()
                .returnResult(String.class);
        assertTrue(resposta.getStatus().value() == statusEsperado,
                "esperava " + statusEsperado + " mas veio " + resposta.getStatus()
                        + " | corpo: " + resposta.getResponseBody());
        return resposta.getResponseBody();
    }

    @Test
    void patchStatus_fluxoCompleto_historicoCom4EventosENotificacao() {
        Long alunoId = criarAluno();
        Long gerenciadorId = criarGerenciador();
        Long editalId = criarEditalAberto(criarPrograma());
        Long submissaoId = putTermoAceite(alunoId, submeter(alunoId, editalId));
        postEnviar(alunoId, submissaoId, 200);

        patchStatus(gerenciadorId, submissaoId, "{\"novoStatus\":\"EM_ANALISE\"}", 200);
        String motivo = "Falta comprovante de matricula";
        patchStatus(gerenciadorId, submissaoId, "{\"novoStatus\":\"PENDENTE_DOCUMENTOS\",\"motivo\":\"" + motivo + "\"}", 200);
        patchStatus(gerenciadorId, submissaoId, "{\"novoStatus\":\"APROVADA\"}", 200);

        rest.get().uri("/api/submissoes/" + submissaoId + "/historico")
                .header("X-Usuario-Id", alunoId.toString())
                .exchange()
                .expectStatus().isEqualTo(200)
                .expectBody()
                .jsonPath("$.length()").isEqualTo(4)
                .jsonPath("$[0].statusAnterior").isEqualTo("PENDENTE_DOCUMENTOS")
                .jsonPath("$[0].statusNovo").isEqualTo("APROVADA");

        // RF17 — notificações criadas nas transições + marcar-lida idempotente
        String inbox = rest.get().uri("/api/notificacoes")
                .header("X-Usuario-Id", alunoId.toString())
                .exchange()
                .expectStatus().isEqualTo(200)
                .returnResult(String.class).getResponseBody();
        assertTrue(inbox.contains(motivo), "motivo aparece na notificação: " + inbox);
        Matcher idLida = Pattern.compile("\"id\":(\\d+)").matcher(inbox);
        assertTrue(idLida.find(), "id de notificação no inbox: " + inbox);
        Long notificacaoId = Long.parseLong(idLida.group(1));
        rest.patch().uri("/api/notificacoes/" + notificacaoId + "/lida")
                .header("X-Usuario-Id", alunoId.toString())
                .exchange().expectStatus().isEqualTo(200);
        rest.patch().uri("/api/notificacoes/" + notificacaoId + "/lida")
                .header("X-Usuario-Id", alunoId.toString())
                .exchange().expectStatus().isEqualTo(200); // idempotente
    }

    @Test
    void patchStatus_rascunhoParaAprovada_retorna409() {
        Long alunoId = criarAluno();
        Long gerenciadorId = criarGerenciador();
        Long editalId = criarEditalAberto(criarPrograma());
        Long submissaoId = submeter(alunoId, editalId);

        String corpo = patchStatus(gerenciadorId, submissaoId, "{\"novoStatus\":\"APROVADA\"}", 409);
        assertTrue(corpo.contains("TRANSICAO_INVALIDA"), "esperava TRANSICAO_INVALIDA: " + corpo);
    }

    @Test
    void patchStatus_semMotivoParaPendente_retorna400() {
        Long alunoId = criarAluno();
        Long gerenciadorId = criarGerenciador();
        Long editalId = criarEditalAberto(criarPrograma());
        Long submissaoId = putTermoAceite(alunoId, submeter(alunoId, editalId));
        postEnviar(alunoId, submissaoId, 200);
        patchStatus(gerenciadorId, submissaoId, "{\"novoStatus\":\"EM_ANALISE\"}", 200);

        String corpo = patchStatus(gerenciadorId, submissaoId, "{\"novoStatus\":\"PENDENTE_DOCUMENTOS\"}", 400);
        assertTrue(corpo.contains("MOTIVO_OBRIGATORIO"), "esperava MOTIVO_OBRIGATORIO: " + corpo);
    }

    @Test
    void patchStatus_headerDeAluno_retorna400() {
        Long alunoId = criarAluno();
        Long editalId = criarEditalAberto(criarPrograma());
        Long submissaoId = submeter(alunoId, editalId);

        String corpo = patchStatus(alunoId, submissaoId, "{\"novoStatus\":\"APROVADA\"}", 400);
        assertTrue(corpo.contains("PERMISSAO_INSUFICIENTE"), "esperava PERMISSAO_INSUFICIENTE: " + corpo);
    }

    @Test
    void patchStatus_semHeader_retorna400() {
        Long alunoId = criarAluno();
        Long editalId = criarEditalAberto(criarPrograma());
        Long submissaoId = submeter(alunoId, editalId);

        String corpo = rest.patch().uri("/api/submissoes/" + submissaoId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"novoStatus\":\"EM_ANALISE\"}")
                .exchange()
                .returnResult(String.class)
                .getResponseBody();
        assertTrue(corpo.contains("ARGUMENTO_INVALIDO"), "esperava ARGUMENTO_INVALIDO: " + corpo);
    }

    /** RN06 (Review Focus #2) — consulta por não-dono responde 404 SEM vazar existência,
     *  em DETALHE, HISTÓRICO e LISTAGEM DE DOCUMENTOS (dono OU gerenciador apenas). */
    @Test
    void consulta_porNaoDono_detalheHistoricoDocumentos_retorna404() {
        Long alunoId = criarAluno();
        Long intrusoId = criarAluno();
        Long editalId = criarEditalAberto(criarPrograma());
        Long submissaoId = putTermoAceite(alunoId, submeter(alunoId, editalId));

        rest.get().uri("/api/submissoes/" + submissaoId)
                .header("X-Usuario-Id", String.valueOf(intrusoId))
                .exchange()
                .expectStatus().isEqualTo(404)
                .expectBody()
                .jsonPath("$.erro").isEqualTo("RECURSO_NAO_ENCONTRADO");

        rest.get().uri("/api/submissoes/" + submissaoId + "/historico")
                .header("X-Usuario-Id", String.valueOf(intrusoId))
                .exchange()
                .expectStatus().isEqualTo(404)
                .expectBody()
                .jsonPath("$.erro").isEqualTo("RECURSO_NAO_ENCONTRADO");

        rest.get().uri("/api/submissoes/" + submissaoId + "/documentos")
                .header("X-Usuario-Id", String.valueOf(intrusoId))
                .exchange()
                .expectStatus().isEqualTo(404)
                .expectBody()
                .jsonPath("$.erro").isEqualTo("RECURSO_NAO_ENCONTRADO");
    }

    @Test
    void gerenciadorPainel_filtroStatus_retorna200EPaginado() {
        Long alunoId = criarAluno();
        Long editalId = criarEditalAberto(criarPrograma());
        Long submissaoId = putTermoAceite(alunoId, submeter(alunoId, editalId));
        postEnviar(alunoId, submissaoId, 200);

        rest.get().uri("/api/gerenciador/submissoes?status=RECEBIDA&editalId=" + editalId)
                .header("X-Usuario-Id", alunoId.toString()) // consulta aberta no cp3 (decisão documentada)
                .exchange()
                .expectStatus().isEqualTo(200)
                .expectBody()
                .jsonPath("$.totalElements").isNumber()
                .jsonPath("$.content[0].programaTitulo").isNotEmpty();
    }
}
