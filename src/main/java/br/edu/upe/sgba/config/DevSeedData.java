package br.edu.upe.sgba.config;

import br.edu.upe.sgba.entity.Aluno;
import br.edu.upe.sgba.entity.Edital;
import br.edu.upe.sgba.entity.ExigenciaEdital;
import br.edu.upe.sgba.entity.PerguntaEdital;
import br.edu.upe.sgba.entity.ProgramaBolsa;
import br.edu.upe.sgba.entity.Usuario;
import br.edu.upe.sgba.entity.enums.CategoriaPrograma;
import br.edu.upe.sgba.entity.enums.FuncaoUsuario;
import br.edu.upe.sgba.entity.enums.Genero;
import br.edu.upe.sgba.entity.enums.StatusPrograma;
import br.edu.upe.sgba.entity.enums.TipoDocumento;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Seed de desenvolvimento (Issue 1 — Checkpoint 3).
 *
 * <p>Ativo <b>somente no profile {@code dev}</b>. Executa uma vez: se já houver
 * usuários no banco, registra no log e não insere nada (idempotente).</p>
 *
 * <p><b>Contrato público do seed (Issues 2–6 usam estes dados em suas demos —
 * não mudar sem avisar no PR):</b></p>
 * <ul>
 *   <li>Usuários: {@code aluno@upe.br} (ALUNO), {@code professor@upe.br}
 *       (PROFESSOR) e {@code gerenciador@upe.br} (GERENCIADOR) — sem auth no
 *       cp3 (D1: header {@code X-Usuario-Id});</li>
 *   <li>Perfil Aluno completo ligado a {@code aluno@upe.br} (CPF/matrícula
 *       fictícias, curso/CRA/endereço preenchidos — RN09 tem snapshot confiável);</li>
 *   <li>1 {@link ProgramaBolsa} (janela aberta — aparece em
 *       {@code GET /api/programas/abertos} da Issue 3) + 4 editais:
 *       <ul>
 *         <li><b>Edital A</b> — 08/2026 — janela ABERTA, 2 perguntas (1
 *             obrigatória) e 2 exigências OPCIONAIS → fluxo feliz da Issue 5
 *             (envio possível sem documentos);</li>
 *         <li><b>Edital B</b> — 09/2026 — janela ABERTA, 1 pergunta obrigatória
 *             e 2 exigências OBRIGATÓRIAS → Issue 6 demonstra RN12 (envio
 *             incompleto → com docs → envio OK);</li>
 *         <li><b>Edital C</b> — 07/2025 — janela ENCERRADA (datas no passado)
 *             → Issue 5 demonstra RN05 (409 {@code FORA_DO_PRAZO});</li>
 *         <li><b>Edital D</b> — 10/2026 — janela ABERTA, sem perguntas/exigências
 *             → infraestrutura do módulo Editais da Issue 4 (criação de
 *             perguntas/exigências e duplicata 409 de {@code numero_edital+ano}).</li>
 *       </ul></li>
 * </ul>
 *
 * <p>Persistência direta via {@link EntityManager} para não acoplar a Issue 1
 * aos repositórios de negócio que pertencem às Issues 2–4 (C2) — o seed é o
 * único dono deste arquivo e evita conflitos de merge entre os PRs paralelos.</p>
 */
@Component
@Profile("dev")
public class DevSeedData implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevSeedData.class);

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(String... args) {
        if (count(Usuario.class) > 0) {
            log.info("[DevSeedData] Seed já aplicado neste banco — nada a inserir. "
                    + "Para recarregar: docker compose down -v && docker compose up -d (volumes zerados).");
            return;
        }

        log.info("[DevSeedData] Aplicando seed de desenvolvimento (profile dev)...");

        Usuario aluno = persistirUsuario("Maria da Silva", "aluno@upe.br", FuncaoUsuario.ALUNO);
        Usuario professor = persistirUsuario("Marcos Peres", "professor@upe.br", FuncaoUsuario.PROFESSOR);
        Usuario gerenciador = persistirUsuario("Gerente Gestor", "gerenciador@upe.br", FuncaoUsuario.GERENCIADOR);

        Aluno perfilAluno = persistirPerfilAluno(aluno);
        ProgramaBolsa programa = persistirPrograma();

        Edital editalA = persistirEditalA(programa);
        Edital editalB = persistirEditalB(programa);
        Edital editalC = persistirEditalC(programa);
        Edital editalD = persistirEditalD(programa);

        persistirPerguntas(editalA, editalB);
        persistirExigencias(editalA, editalB);

        log.info("""
                [DevSeedData] Seed aplicado. Contrato público (Issues 2–6): \
                  Usuarios -> aluno(id={})=aluno@upe.br | professor(id={})=professor@upe.br | gerenciador(id={})=gerenciador@upe.br \
                  Aluno: perfil(id={}) ligado a usuario(id={}) \
                  Programas -> id={} = '{}' (janela aberta) \
                  Editais -> A(id={}, 08/2026, aberto) | B(id={}, 09/2026, aberto, exigencias obrigatorias) \
                | C(id={}, 07/2025, janela encerrada) | D(id={}, 10/2026, aberto, sem formulario)
                """,
                aluno.getId(), professor.getId(), gerenciador.getId(),
                perfilAluno.getId(), perfilAluno.getUsuario().getId(),
                programa.getId(), programa.getTitulo(),
                editalA.getId(), editalB.getId(), editalC.getId(), editalD.getId());
    }

    // ------------------------------- Helpers --------------------------------

    private long count(Class<?> entity) {
        return em.createQuery("SELECT COUNT(x) FROM " + entity.getSimpleName() + " x", Long.class).getSingleResult();
    }

    private Usuario persistirUsuario(String nome, String email, FuncaoUsuario funcao) {
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(email);
        u.setSenhaHash("cp3-sem-auth-substituido-no-cp4");
        u.setFuncao(funcao);
        u.setCriadoEm(LocalDateTime.now());
        em.persist(u);
        return u;
    }

    private Aluno persistirPerfilAluno(Usuario usuario) {
        Aluno a = new Aluno();
        a.setUsuario(usuario);
        a.setCpf("00000000111");
        a.setRg("99999999-9");
        a.setDataNascimento(LocalDate.of(2003, 5, 14));
        a.setNacionalidade("Brasileira");
        a.setGenero(Genero.FEMININO);
        a.setTelefone("81990000001");
        a.setMatricula("20230001");
        a.setCurso("Sistemas de Informação");
        a.setPeriodoAtual((short) 6);
        a.setCraAtual(new BigDecimal("8.50"));
        a.setUnidadeAcademica("UPE - Campus Garanhuns");
        a.setSemestreIngresso("2023.1");
        a.setLogradouro("Rua do Estudante");
        a.setNumero("123");
        a.setBairro("Centro");
        a.setCidade("Garanhuns");
        a.setUf("PE");
        a.setCep("55290000");
        em.persist(a);
        return a;
    }

    private ProgramaBolsa persistirPrograma() {
        ProgramaBolsa p = new ProgramaBolsa();
        p.setTitulo("Programa de Iniciacao Cientifica (PIBIC)");
        p.setCategoria(CategoriaPrograma.INICIACAO_CIENTIFICA);
        p.setDescricao("Bolsa de iniciacao cientifica para alunos ativos da UPE (dados de demonstracao).");
        p.setValorMensal(new BigDecimal("900.00"));
        p.setVagas((short) 10);
        p.setPeriodoLetivo("2026.1");
        p.setDataAbertura(LocalDateTime.now().minusDays(30));
        p.setDataLimite(LocalDateTime.now().plusDays(90));
        p.setStatus(StatusPrograma.ABERTO);
        em.persist(p);
        return p;
    }

    private Edital editalBase(ProgramaBolsa programa, String numero, short ano, LocalDate publicacao,
                              LocalDateTime inicio, LocalDateTime fim) {
        Edital e = new Edital();
        e.setPrograma(programa);
        e.setNumeroEdital(numero);
        e.setAno(ano);
        e.setDataPublicacao(publicacao);
        e.setDataInicioSubmissoes(inicio);
        e.setDataFimSubmissoes(fim);
        e.setCaminhoArquivoPdf(null); // v3: preenchido apenas no upload (Issue 4)
        em.persist(e);
        return e;
    }

    private Edital persistirEditalA(ProgramaBolsa programa) {
        // Janela aberta: agora-20d até agora+60d
        return editalBase(programa, "08", (short) 2026, LocalDate.now().minusDays(25),
                LocalDateTime.now().minusDays(20), LocalDateTime.now().plusDays(60));
    }

    private Edital persistirEditalB(ProgramaBolsa programa) {
        return editalBase(programa, "09", (short) 2026, LocalDate.now().minusDays(25),
                LocalDateTime.now().minusDays(20), LocalDateTime.now().plusDays(60));
    }

    private Edital persistirEditalC(ProgramaBolsa programa) {
        // Janela encerrada no passado — demonstra RN05 (409 FORA_DO_PRAZO)
        return editalBase(programa, "07", (short) 2025, LocalDate.of(2025, 8, 1),
                LocalDateTime.of(2025, 8, 10, 0, 0), LocalDateTime.of(2025, 9, 30, 23, 59, 59));
    }

    private Edital persistirEditalD(ProgramaBolsa programa) {
        return editalBase(programa, "10", (short) 2026, LocalDate.now().minusDays(10),
                LocalDateTime.now().minusDays(5), LocalDateTime.now().plusDays(75));
    }

    private void persistirPerguntas(Edital editalA, Edital editalB) {
        PerguntaEdital a1 = new PerguntaEdital();
        a1.setEdital(editalA);
        a1.setEnunciado("Descreva sua experiencia anterior com pesquisa.");
        a1.setObrigatoria(true);
        a1.setOrdemExibicao((short) 1);
        em.persist(a1);

        PerguntaEdital a2 = new PerguntaEdital();
        a2.setEdital(editalA);
        a2.setEnunciado("Possui computador dedicado ao projeto?");
        a2.setObrigatoria(false); // opcional
        a2.setOrdemExibicao((short) 2);
        em.persist(a2);

        PerguntaEdital b1 = new PerguntaEdital();
        b1.setEdital(editalB);
        b1.setEnunciado("Detalhe o plano de trabalho pretendido.");
        b1.setObrigatoria(true);
        b1.setOrdemExibicao((short) 1);
        em.persist(b1);
    }

    private void persistirExigencias(Edital editalA, Edital editalB) {
        // Edital A: 2 exigências OPCIONAIS (envio possível sem documentos)
        ExigenciaEdital a1 = new ExigenciaEdital();
        a1.setEdital(editalA);
        a1.setTipoDocumento(TipoDocumento.HISTORICO_ESCOLAR);
        a1.setObrigatorio(false);
        em.persist(a1);

        ExigenciaEdital a2 = new ExigenciaEdital();
        a2.setEdital(editalA);
        a2.setTipoDocumento(TipoDocumento.COMPROVANTE_MATRICULA);
        a2.setObrigatorio(false);
        em.persist(a2);

        // Edital B: 2 exigências OBRIGATÓRIAS (RN12 — Issue 6)
        ExigenciaEdital b1 = new ExigenciaEdital();
        b1.setEdital(editalB);
        b1.setTipoDocumento(TipoDocumento.HISTORICO_ESCOLAR);
        b1.setObrigatorio(true);
        em.persist(b1);

        ExigenciaEdital b2 = new ExigenciaEdital();
        b2.setEdital(editalB);
        b2.setTipoDocumento(TipoDocumento.COMPROVANTE_MATRICULA);
        b2.setObrigatorio(true);
        em.persist(b2);
    }
}
