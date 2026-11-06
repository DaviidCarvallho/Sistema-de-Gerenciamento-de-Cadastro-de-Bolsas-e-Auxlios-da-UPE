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
}
