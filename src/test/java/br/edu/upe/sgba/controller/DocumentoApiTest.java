package br.edu.upe.sgba.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import br.edu.upe.sgba.entity.Edital;
import br.edu.upe.sgba.entity.ExigenciaEdital;
import br.edu.upe.sgba.entity.PerguntaEdital;
import br.edu.upe.sgba.entity.ProgramaBolsa;
import br.edu.upe.sgba.entity.enums.CategoriaPrograma;
import br.edu.upe.sgba.entity.enums.StatusPrograma;
import br.edu.upe.sgba.entity.enums.TipoDocumento;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Módulo Documentos (Issue 6): upload multipart (RF09), RN03 (editável em
 * RASCUNHO/PENDENTE_DOCUMENTOS), RN11 (OUTRO exige descrição), download com os
 * mesmos bytes, delete e validação do gerenciador (RF15, §9.2), D7 (5 MB / PDF).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DocumentoApiTest {

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
            p.setTitulo("Programa Docs");
            p.setCategoria(CategoriaPrograma.INICIACAO_CIENTIFICA);
            p.setValorMensal(new java.math.BigDecimal("500.00"));
            p.setVagas((short) 5);
            p.setPeriodoLetivo("2026.2");
            p.setDataAbertura(LocalDateTime.now().minusDays(1));
            p.setDataLimite(LocalDateTime.now().plusDays(30));
            p.setStatus(StatusPrograma.ABERTO);
            em.persist(p);
            return p.getId();
        });
    }

    private Long criarEditalAberto(Long programaId) {
        return extrairId(rest.post().uri("/api/editais")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"programaId\":" + programaId + ",\"numeroEdital\":\"T" + sufixoUnico()
                        + "\",\"ano\":2026,\"dataInicioSubmissoes\":\"" + LocalDateTime.now().minusDays(1)
                        + "\",\"dataFimSubmissoes\":\"" + LocalDateTime.now().plusDays(30) + "\"}")
                .exchange()
                .expectStatus().isEqualTo(201)
                .returnResult(String.class).getResponseBody());
    }

    private Long criarAluno() {
        Long usuarioId = extrairId(rest.post().uri("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"nome\":\"Aluno Docs\",\"email\":\"al." + sufixoUnico()
                        + "@upe.br\",\"senha\":\"x\",\"funcao\":\"ALUNO\"}")
                .exchange()
                .expectStatus().isEqualTo(201)
                .returnResult(String.class).getResponseBody());
        String numeros = String.format("%010d",
                Math.abs(new java.security.SecureRandom().nextLong() % 10_000_000_000L));
        rest.post().uri("/api/alunos")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"usuarioId\":" + usuarioId + ",\"cpf\":\"1" + numeros + "\",\"matricula\":\"9"
                        + numeros + "\",\"craAtual\":7.2,\"curso\":\"ADS\",\"periodoAtual\":4,"
                        + "\"unidadeAcademica\":\"UPE Campus\"}")
                .exchange()
                .expectStatus().isEqualTo(201);
        return usuarioId;
    }

    private Long criarGerenciador() {
        return extrairId(rest.post().uri("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"nome\":\"Gerente Docs\",\"email\":\"gr." + sufixoUnico()
                        + "@upe.br\",\"senha\":\"x\",\"funcao\":\"GERENCIADOR\"}")
                .exchange()
                .expectStatus().isEqualTo(201)
                .returnResult(String.class).getResponseBody());
    }

    /** Edital com 1 exigência obrigatória HISTORICO_ESCOLAR (rota da Issue 4). */
    private Long criarEditalComExigenciaObrigatoria(Long programaId) {
        Long editalId = criarEditalAberto(programaId);
        rest.post().uri("/api/editais/" + editalId + "/exigencias")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"tipoDocumento\":\"HISTORICO_ESCOLAR\",\"obrigatorio\":true}")
                .exchange()
                .expectStatus().isEqualTo(201);
        return editalId;
    }

    private Long criarPerguntaObrigatoria(Long editalId) {
        return new TransactionTemplate(tm).execute(status -> {
            PerguntaEdital p = new PerguntaEdital();
            p.setEdital(em.find(Edital.class, editalId));
            p.setEnunciado("Responde? " + sufixoUnico());
            p.setObrigatoria(true);
            p.setOrdemExibicao((short) 1);
            em.persist(p);
            return p.getId();
        });
    }

    /** Fluxo: rascunho → responde pergunta(s) → termoAceite (não envia). */
    private Long submissaoEmRascunho(Long alunoId, Long editalId, Long perguntaId) {
        Long submissaoId = extrairId(rest.post().uri("/api/submissoes")
                .header("X-Usuario-Id", String.valueOf(alunoId))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"editalId\":" + editalId + ",\"tituloProjeto\":\"Projeto " + sufixoUnico()
                        + "\",\"projetoResumo\":\"r\",\"planoTrabalho\":\"p\"}")
                .exchange()
                .expectStatus().isEqualTo(201)
                .returnResult(String.class).getResponseBody());
        if (perguntaId != null) {
            rest.put().uri("/api/submissoes/" + submissaoId)
                    .header("X-Usuario-Id", String.valueOf(alunoId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"respostas\":[{\"perguntaId\":" + perguntaId
                            + ",\"respostaTexto\":\"resp\"}],\"termoAceite\":true}")
                    .exchange()
                    .expectStatus().isEqualTo(200);
        } else {
            rest.put().uri("/api/submissoes/" + submissaoId)
                    .header("X-Usuario-Id", String.valueOf(alunoId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"termoAceite\":true}")
                    .exchange()
                    .expectStatus().isEqualTo(200);
        }
        return submissaoId;
    }

    /** Configurador de: aluno + edital com exigência obrigatória + submissão em rascunho completo. */
    private record Cenario(Long alunoId, Long gerenciadorId, Long editalId, Long submissaoId, Long perguntaId) {
    }

    private Cenario cenarioCompleto() {
        Long studentId = criarAluno();
        Long eid = criarEditalComExigenciaObrigatoria(criarPrograma());
        Long pid = criarPerguntaObrigatoria(eid);
        return new Cenario(studentId, criarGerenciador(), eid,
                submissaoEmRascunho(studentId, eid, pid), pid);
    }

    // ------------------------------ RF09 — upload ------------------------------

    @Test
    void upload_pdfValido_retorna201ComPendente() {
        Cenario c = cenarioCompleto();
        byte[] pdf = "%PDF-1.4 conteudo valido de teste".getBytes(StandardCharsets.UTF_8);

        rest.post().uri("/api/submissoes/" + c.submissaoId() + "/documentos")
                .header("X-Usuario-Id", String.valueOf(c.alunoId()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parteArquivoPdfComTipo(pdf, "historico.pdf", "application/pdf",
                        TipoDocumento.HISTORICO_ESCOLAR.name(), null))
                .exchange()
                .expectStatus().isEqualTo(201)
                .expectBody()
                .jsonPath("$.statusValidacao").isEqualTo("PENDENTE")
                .jsonPath("$.tipoDocumento").isEqualTo("HISTORICO_ESCOLAR")
                .jsonPath("$.nomeOriginal").isEqualTo("historico.pdf");

        rest.get().uri("/api/submissoes/" + c.submissaoId() + "/documentos")
                .header("X-Usuario-Id", String.valueOf(c.alunoId()))
                .exchange()
                .expectStatus().isEqualTo(200)
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1);
    }

    @Test
    void upload_naoPdf_retorna400() {
        Cenario c = cenarioCompleto();
        byte[] textoPdfFalso = "conteudo texto".getBytes(StandardCharsets.UTF_8);

        String corpo = rest.post().uri("/api/submissoes/" + c.submissaoId() + "/documentos")
                .header("X-Usuario-Id", String.valueOf(c.alunoId()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parteArquivoPdfComTipo(textoPdfFalso, "notas.txt", "text/plain",
                        TipoDocumento.HISTORICO_ESCOLAR.name(), null))
                .exchange()
                .expectStatus().isEqualTo(400)
                .returnResult(String.class).getResponseBody();
        assertTrue(corpo.contains("ARQUIVO_INVALIDO"), "PDF obrigatório: " + corpo);
    }

    @Test
    void upload_tipoOutro_semDescricao_retorna400() {
        Cenario c = cenarioCompleto();
        byte[] pdf = "%PDF-1.4".getBytes(StandardCharsets.UTF_8);

        String corpo = rest.post().uri("/api/submissoes/" + c.submissaoId() + "/documentos")
                .header("X-Usuario-Id", String.valueOf(c.alunoId()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parteArquivoPdfComTipo(pdf, "outro.pdf", "application/pdf",
                        TipoDocumento.OUTRO.name(), null))
                .exchange()
                .expectStatus().isEqualTo(400)
                .returnResult(String.class).getResponseBody();
        assertTrue(corpo.contains("DESCRICAO_OBRIGATORIA"), "RN11: " + corpo);
    }

    @Test
    void upload_emSubmissaoNaoEditavel_retorna409() {
        Long alunoId = criarAluno();
        Long gerenciadorId = criarGerenciador();
        Long editalId = criarEditalAberto(criarPrograma());          // sem perguntas/exigências → envio possível
        Long submissaoId = submissaoEmRascunho(alunoId, editalId, null);
        // envia a submissão → RECEBIDA (aluno não edita mais; RN03)
        rest.post().uri("/api/submissoes/" + submissaoId + "/enviar")
                .header("X-Usuario-Id", String.valueOf(alunoId))
                .exchange()
                .expectStatus().isEqualTo(200);

        byte[] pdf = "%PDF-1.4".getBytes(StandardCharsets.UTF_8);
        String corpo = rest.post().uri("/api/submissoes/" + submissaoId + "/documentos")
                .header("X-Usuario-Id", String.valueOf(alunoId))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parteArquivoPdfComTipo(pdf, "atrasado.pdf", "application/pdf",
                        TipoDocumento.OUTRO.name(), "comprovante extra"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .returnResult(String.class).getResponseBody();
        assertTrue(corpo.contains("STATUS_NAO_EDITAVEL"), "RN03: " + corpo);
    }

    @Test
    void upload_acimaDe5MB_retorna400() {
        Cenario c = cenarioCompleto();
        byte[] grande = new byte[5 * 1024 * 1024 + 1];
        grande[0] = '%'; grande[1] = 'P'; grande[2] = 'D'; grande[3] = 'F';

        String corpo = rest.post().uri("/api/submissoes/" + c.submissaoId() + "/documentos")
                .header("X-Usuario-Id", String.valueOf(c.alunoId()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parteArquivoPdfComTipo(grande, "grande.pdf", "application/pdf",
                        TipoDocumento.HISTORICO_ESCOLAR.name(), null))
                .exchange()
                .expectStatus().isEqualTo(400)
                .returnResult(String.class).getResponseBody();
        assertTrue(corpo.contains("ARQUIVO_MUITO_GRANDE"), "D7 5MB: " + corpo);
    }

    @Test
    void upload_eDownload_devolveMesmosBytes_eDeleteRemove() {
        Cenario c = cenarioCompleto();
        byte[] pdf = ("%PDF-1.4 bytes distintos " + sufixoUnico()).getBytes(StandardCharsets.UTF_8);
        String corpoUpload = rest.post().uri("/api/submissoes/" + c.submissaoId() + "/documentos")
                .header("X-Usuario-Id", String.valueOf(c.alunoId()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parteArquivoPdfComTipo(pdf, "historico-original.pdf", "application/pdf",
                        TipoDocumento.HISTORICO_ESCOLAR.name(), null))
                .exchange()
                .expectStatus().isEqualTo(201)
                .returnResult(String.class).getResponseBody();
        Long documentoId = extrairId(corpoUpload);

        byte[] baixado = rest.get().uri("/api/documentos/" + documentoId + "/download")
                .header("X-Usuario-Id", String.valueOf(c.alunoId()))
                .exchange()
                .expectStatus().isEqualTo(200)
                .returnResult(byte[].class).getResponseBody();
        assertArrayEquals(pdf, baixado);

        rest.delete().uri("/api/documentos/" + documentoId)
                .header("X-Usuario-Id", String.valueOf(c.alunoId()))
                .exchange()
                .expectStatus().isEqualTo(204);

        rest.get().uri("/api/documentos/" + documentoId + "/download")
                .header("X-Usuario-Id", String.valueOf(c.alunoId()))
                .exchange()
                .expectStatus().isEqualTo(404);
    }

    @Test
    void upload_porNaoDono_retorna404() {
        Cenario c = cenarioCompleto();
        Long intruso = criarAluno();
        byte[] pdf = "%PDF-1.4".getBytes(StandardCharsets.UTF_8);

        rest.post().uri("/api/submissoes/" + c.submissaoId() + "/documentos")
                .header("X-Usuario-Id", String.valueOf(intruso))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parteArquivoPdfComTipo(pdf, "x.pdf", "application/pdf",
                        TipoDocumento.HISTORICO_ESCOLAR.name(), null))
                .exchange()
                .expectStatus().isEqualTo(404);
    }

    @Test
    void upload_tipoDocumentoInvalido_retorna400() {
        Cenario c = cenarioCompleto();
        byte[] pdf = "%PDF-1.4".getBytes(StandardCharsets.UTF_8);

        String corpo = rest.post().uri("/api/submissoes/" + c.submissaoId() + "/documentos")
                .header("X-Usuario-Id", String.valueOf(c.alunoId()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parteArquivoPdfComTipo(pdf, "x.pdf", "application/pdf",
                        "RECEITUARIA", null))
                .exchange()
                .expectStatus().isEqualTo(400)
                .returnResult(String.class).getResponseBody();
        assertTrue(corpo.contains("tipoDocumento"), "enum inválido: " + corpo);
    }

    // ------------------------------ RN03 — PENDENTE_DOCUMENTOS permite upload ------------------------------

    @Test
    void upload_emPendenteDocumentos_permite201() {
        Long alunoId = criarAluno();
        Long gerenciadorId = criarGerenciador();
        Long editalId = criarEditalAberto(criarPrograma());          // sem perguntas/exigências → envio possível
        Long submissaoId = submissaoEmRascunho(alunoId, editalId, null);
        rest.post().uri("/api/submissoes/" + submissaoId + "/enviar")
                .header("X-Usuario-Id", String.valueOf(alunoId))
                .exchange()
                .expectStatus().isEqualTo(200);
        rest.patch().uri("/api/submissoes/" + submissaoId + "/status")
                .header("X-Usuario-Id", String.valueOf(gerenciadorId))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"novoStatus\":\"EM_ANALISE\"}")
                .exchange()
                .expectStatus().isEqualTo(200);
        rest.patch().uri("/api/submissoes/" + submissaoId + "/status")
                .header("X-Usuario-Id", String.valueOf(gerenciadorId))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"novoStatus\":\"PENDENTE_DOCUMENTOS\",\"motivo\":\"Falta historico\"}")
                .exchange()
                .expectStatus().isEqualTo(200);

        byte[] pdf = "%PDF-1.4".getBytes(StandardCharsets.UTF_8);
        String corpo = rest.post().uri("/api/submissoes/" + submissaoId + "/documentos")
                .header("X-Usuario-Id", String.valueOf(alunoId))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parteArquivoPdfComTipo(pdf, "historico.pdf", "application/pdf",
                        TipoDocumento.OUTRO.name(), "Historico reenviado"))
                .exchange()
                .expectStatus().isEqualTo(201)
                .returnResult(String.class).getResponseBody();
        assertTrue(corpo.contains("PENDENTE"), "novo documento nasce PENDENTE: " + corpo);
    }

    // ------------------------------ RF15 — validação do gerenciador ------------------------------

    @Test
    void validacao_incompletoSemObservacao_retorna400() {
        Cenario c = cenarioCompleto();
        byte[] pdf = "%PDF-1.4".getBytes(StandardCharsets.UTF_8);
        Long documentoId = extrairId(rest.post().uri("/api/submissoes/" + c.submissaoId() + "/documentos")
                .header("X-Usuario-Id", String.valueOf(c.alunoId()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parteArquivoPdfComTipo(pdf, "h.pdf", "application/pdf",
                        TipoDocumento.HISTORICO_ESCOLAR.name(), null))
                .exchange()
                .expectStatus().isEqualTo(201)
                .returnResult(String.class).getResponseBody());

        String corpo = rest.patch().uri("/api/documentos/" + documentoId + "/status")
                .header("X-Usuario-Id", String.valueOf(c.gerenciadorId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"statusValidacao\":\"INCOMPLETO\"}")
                .exchange()
                .expectStatus().isEqualTo(400)
                .returnResult(String.class).getResponseBody();
        assertTrue(corpo.contains("OBSERVACAO_OBRIGATORIA"), "RF15 com observação: " + corpo);

        rest.patch().uri("/api/documentos/" + documentoId + "/status")
                .header("X-Usuario-Id", String.valueOf(c.gerenciadorId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"statusValidacao\":\"INCOMPLETO\",\"observacaoValidacao\":\"PDF ilegível\"}")
                .exchange()
                .expectStatus().isEqualTo(200)
                .expectBody()
                .jsonPath("$.statusValidacao").isEqualTo("INCOMPLETO")
                .jsonPath("$.observacaoValidacao").isEqualTo("PDF ilegível");

        rest.get().uri("/api/submissoes/" + c.submissaoId() + "/documentos")
                .header("X-Usuario-Id", String.valueOf(c.alunoId()))
                .exchange()
                .expectStatus().isEqualTo(200)
                .expectBody()
                .jsonPath("$[0].statusValidacao").isEqualTo("INCOMPLETO");
    }

    // ------------------------------ suporte multipart ------------------------------

    private MultiValueMap<String, Object> parteArquivoPdfComTipo(byte[] conteudo, String nome,
            String contentType, String tipoDocumento, String descricaoPersonalizada) {
        HttpHeaders c = new HttpHeaders();
        c.setContentType(MediaType.parseMediaType(contentType));
        ByteArrayResource r = new ByteArrayResource(conteudo) {
            @Override
            public String getFilename() {
                return nome;
            }
        };
        MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
        partes.add("arquivo", new HttpEntity<>(r, c));
        partes.add("tipoDocumento", tipoDocumento);
        if (descricaoPersonalizada != null) {
            partes.add("descricaoPersonalizada", descricaoPersonalizada);
        }
        return partes;
    }
}
