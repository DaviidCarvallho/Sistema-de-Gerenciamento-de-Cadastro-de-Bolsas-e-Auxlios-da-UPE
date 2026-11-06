package br.edu.upe.sgba.controller;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import br.edu.upe.sgba.entity.ProgramaBolsa;
import br.edu.upe.sgba.entity.enums.CategoriaPrograma;
import br.edu.upe.sgba.entity.enums.StatusPrograma;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EditalApiTest {

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

    private static String numeroUnico() {
        return "T" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String jsonEdital(Long programaId, String numero, String inicio, String fim) {
        return "{\"programaId\":" + programaId + ",\"numeroEdital\":\"" + numero + "\",\"ano\":2026,"
                + "\"dataInicioSubmissoes\":\"" + inicio + "\",\"dataFimSubmissoes\":\"" + fim + "\"}";
    }

    private static String jsonEdital(Long programaId, String numero) {
        return jsonEdital(programaId, numero, "2026-11-01T00:00:00", "2026-12-31T23:59:59");
    }

    private static long extrairId(String corpo) {
        Matcher m = Pattern.compile("\"id\":(\\d+)").matcher(corpo);
        assertTrue(m.find());
        return Long.parseLong(m.group(1));
    }

    private long criarEdital() {
        String corpo = rest.post().uri("/api/editais").contentType(MediaType.APPLICATION_JSON)
                .body(jsonEdital(criarPrograma(), numeroUnico()))
                .exchange()
                .expectStatus().isEqualTo(201)
                .returnResult(String.class).getResponseBody();
        return extrairId(corpo);
    }

   private static MultiValueMap<String, Object> parteArquivo(byte[] conteudo, String nome, MediaType tipo) {
    HttpHeaders cabecalhos = new HttpHeaders();
    cabecalhos.setContentType(tipo);
    ByteArrayResource recurso = new ByteArrayResource(conteudo) {
        @Override
        public String getFilename() {
            return nome;
        }
    };
    MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
    partes.add("file", new HttpEntity<>(recurso, cabecalhos));
    return partes;
}

    // ------------------------------- editais -------------------------------

    @Test
    void criarEdital_valido_retorna201ComLocationEPublicacaoDefault() {
        Long programaId = criarPrograma();
        rest.post().uri("/api/editais").contentType(MediaType.APPLICATION_JSON)
                .body(jsonEdital(programaId, numeroUnico()))
                .exchange()
                .expectStatus().isEqualTo(201)
                .expectHeader().exists("Location")
                .expectBody()
                .jsonPath("$.programaId").isEqualTo(programaId)
                .jsonPath("$.dataPublicacao").isNotEmpty()
                .jsonPath("$.caminhoArquivoPdf").isEmpty();
    }

    @Test
    void criarEdital_numeroEAnoDuplicados_retorna409() {
        Long programaId = criarPrograma();
        String numero = numeroUnico();
        rest.post().uri("/api/editais").contentType(MediaType.APPLICATION_JSON)
                .body(jsonEdital(programaId, numero)).exchange().expectStatus().isEqualTo(201);
        rest.post().uri("/api/editais").contentType(MediaType.APPLICATION_JSON)
                .body(jsonEdital(programaId, numero)).exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.erro").isEqualTo("EDITAL_DUPLICADO");
    }

    @Test
    void criarEdital_janelaInvertida_retorna400() {
        rest.post().uri("/api/editais").contentType(MediaType.APPLICATION_JSON)
                .body(jsonEdital(criarPrograma(), numeroUnico(), "2026-12-31T00:00:00", "2026-11-01T00:00:00"))
                .exchange()
                .expectStatus().isEqualTo(400)
                .expectBody().jsonPath("$.erro").isEqualTo("JANELA_INVALIDA");
    }

    @Test
    void criarEdital_programaInexistente_retorna404() {
        rest.post().uri("/api/editais").contentType(MediaType.APPLICATION_JSON)
                .body(jsonEdital(999999L, numeroUnico()))
                .exchange()
                .expectStatus().isEqualTo(404)
                .expectBody().jsonPath("$.erro").isEqualTo("RECURSO_NAO_ENCONTRADO");
    }

    @Test
    void buscarEdital_inexistente_retorna404() {
        rest.get().uri("/api/editais/999999").exchange().expectStatus().isEqualTo(404);
    }

    @Test
    void listarEditais_filtraPorPrograma() {
        Long programaId = criarPrograma();
        String corpo = rest.post().uri("/api/editais").contentType(MediaType.APPLICATION_JSON)
                .body(jsonEdital(programaId, numeroUnico())).exchange()
                .expectStatus().isEqualTo(201)
                .returnResult(String.class).getResponseBody();
        long id = extrairId(corpo);

        rest.get().uri("/api/editais?programaId=" + programaId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[?(@.id==" + id + ")]").exists();
    }

    @Test
    void atualizarEdital_ok_e_duplicadoRetorna409() {
        Long programaId = criarPrograma();
        String n1 = numeroUnico();
        String n2 = numeroUnico();
        rest.post().uri("/api/editais").contentType(MediaType.APPLICATION_JSON)
                .body(jsonEdital(programaId, n1)).exchange().expectStatus().isEqualTo(201);
        long id2 = extrairId(rest.post().uri("/api/editais").contentType(MediaType.APPLICATION_JSON)
                .body(jsonEdital(programaId, n2)).exchange().expectStatus().isEqualTo(201)
                .returnResult(String.class).getResponseBody());

        // muda a janela mantendo o próprio número: não é duplicata de si mesmo
        rest.put().uri("/api/editais/" + id2).contentType(MediaType.APPLICATION_JSON)
                .body(jsonEdital(programaId, n2, "2026-11-02T00:00:00", "2027-01-31T23:59:59"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.dataFimSubmissoes").isEqualTo("2027-01-31T23:59:59");

        // tenta usar o número do outro edital -> 409
        rest.put().uri("/api/editais/" + id2).contentType(MediaType.APPLICATION_JSON)
                .body(jsonEdital(programaId, n1))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    // ------------------------------ perguntas ------------------------------

    @Test
    void perguntas_crud_ordemUnicaEFormularioOrdenado() {
        long editalId = criarEdital();

        // cria primeiro a ordem 2 e depois a 1: o formulário deve vir ordenado (1, 2)
        rest.post().uri("/api/editais/" + editalId + "/perguntas").contentType(MediaType.APPLICATION_JSON)
                .body("{\"enunciado\":\"Segunda\",\"obrigatoria\":false,\"ordemExibicao\":2}")
                .exchange().expectStatus().isEqualTo(201).expectHeader().exists("Location");
        long p1 = extrairId(rest.post().uri("/api/editais/" + editalId + "/perguntas")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"enunciado\":\"Primeira\",\"obrigatoria\":true,\"ordemExibicao\":1}")
                .exchange().expectStatus().isEqualTo(201)
                .returnResult(String.class).getResponseBody());

        // ordem repetida -> 409
        rest.post().uri("/api/editais/" + editalId + "/perguntas").contentType(MediaType.APPLICATION_JSON)
                .body("{\"enunciado\":\"Outra\",\"obrigatoria\":true,\"ordemExibicao\":1}")
                .exchange().expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.erro").isEqualTo("ORDEM_JA_USADA");

        rest.get().uri("/api/editais/" + editalId + "/formulario")
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.perguntas[0].ordemExibicao").isEqualTo(1)
                .jsonPath("$.perguntas[1].ordemExibicao").isEqualTo(2)
                .jsonPath("$.exigencias").isEmpty();

        // PUT para uma ordem ocupada -> 409; mantendo a própria ordem -> 200
        rest.put().uri("/api/editais/" + editalId + "/perguntas/" + p1).contentType(MediaType.APPLICATION_JSON)
                .body("{\"enunciado\":\"Primeira\",\"obrigatoria\":true,\"ordemExibicao\":2}")
                .exchange().expectStatus().isEqualTo(409);
        rest.put().uri("/api/editais/" + editalId + "/perguntas/" + p1).contentType(MediaType.APPLICATION_JSON)
                .body("{\"enunciado\":\"Primeira (editada)\",\"obrigatoria\":false,\"ordemExibicao\":1}")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.enunciado").isEqualTo("Primeira (editada)");

        // DELETE -> 204 e depois 404
        rest.delete().uri("/api/editais/" + editalId + "/perguntas/" + p1)
                .exchange().expectStatus().isEqualTo(204);
        rest.delete().uri("/api/editais/" + editalId + "/perguntas/" + p1)
                .exchange().expectStatus().isEqualTo(404);
    }

    @Test
    void pergunta_emEditalInexistente_retorna404() {
        rest.post().uri("/api/editais/999999/perguntas").contentType(MediaType.APPLICATION_JSON)
                .body("{\"enunciado\":\"X\",\"obrigatoria\":true,\"ordemExibicao\":1}")
                .exchange().expectStatus().isEqualTo(404);
    }

    // ------------------------------ exigências ------------------------------

    @Test
    void exigencias_crud_tipoUnicoPorEdital() {
        long editalId = criarEdital();

        long x1 = extrairId(rest.post().uri("/api/editais/" + editalId + "/exigencias")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"tipoDocumento\":\"HISTORICO_ESCOLAR\",\"obrigatorio\":true}")
                .exchange().expectStatus().isEqualTo(201).expectHeader().exists("Location")
                .returnResult(String.class).getResponseBody());

        rest.post().uri("/api/editais/" + editalId + "/exigencias").contentType(MediaType.APPLICATION_JSON)
                .body("{\"tipoDocumento\":\"HISTORICO_ESCOLAR\",\"obrigatorio\":false}")
                .exchange().expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.erro").isEqualTo("TIPO_JA_EXIGIDO");

        rest.post().uri("/api/editais/" + editalId + "/exigencias").contentType(MediaType.APPLICATION_JSON)
                .body("{\"tipoDocumento\":\"XYZ\",\"obrigatorio\":false}")
                .exchange().expectStatus().isEqualTo(400);

        rest.put().uri("/api/editais/" + editalId + "/exigencias/" + x1).contentType(MediaType.APPLICATION_JSON)
                .body("{\"tipoDocumento\":\"HISTORICO_ESCOLAR\",\"obrigatorio\":false}")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.obrigatorio").isEqualTo(false);

        rest.delete().uri("/api/editais/" + editalId + "/exigencias/" + x1)
                .exchange().expectStatus().isEqualTo(204);
        rest.delete().uri("/api/editais/" + editalId + "/exigencias/" + x1)
                .exchange().expectStatus().isEqualTo(404);
    }

    // -------------------------------- arquivo --------------------------------

    @Test
    void arquivo_uploadPdf_e_download_devolveOMesmoConteudo() {
        long editalId = criarEdital();
        byte[] pdf = "%PDF-1.4\n%conteudo de teste\n%%EOF".getBytes(StandardCharsets.US_ASCII);

        rest.post().uri("/api/editais/" + editalId + "/arquivo")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parteArquivo(pdf, "edital.pdf", MediaType.APPLICATION_PDF))
                .exchange()
                .expectStatus().isEqualTo(201)
                .expectBody().jsonPath("$.caminhoArquivo").isNotEmpty();

        byte[] baixado = rest.get().uri("/api/editais/" + editalId + "/arquivo")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PDF)
                .returnResult(byte[].class).getResponseBody();
        assertArrayEquals(pdf, baixado);

        // a ficha do edital passa a mostrar o caminho
        rest.get().uri("/api/editais/" + editalId)
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.caminhoArquivoPdf").isNotEmpty();
    }

    @Test
    void arquivo_naoPdf_retorna400() {
        long editalId = criarEdital();
        rest.post().uri("/api/editais/" + editalId + "/arquivo")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parteArquivo("texto".getBytes(StandardCharsets.US_ASCII), "nota.txt",
                        MediaType.TEXT_PLAIN))
                .exchange()
                .expectStatus().isEqualTo(400)
                .expectBody().jsonPath("$.erro").isEqualTo("ARQUIVO_INVALIDO");
    }

    @Test
    void arquivo_emEditalInexistente_retorna404() {
        rest.post().uri("/api/editais/999999/arquivo")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parteArquivo("%PDF-1.4".getBytes(StandardCharsets.US_ASCII), "edital.pdf",
                        MediaType.APPLICATION_PDF))
                .exchange()
                .expectStatus().isEqualTo(404);
    }

    @Test
    void arquivo_downloadSemUpload_retorna404() {
        long editalId = criarEdital();
        rest.get().uri("/api/editais/" + editalId + "/arquivo")
                .exchange().expectStatus().isEqualTo(404);
    }
}