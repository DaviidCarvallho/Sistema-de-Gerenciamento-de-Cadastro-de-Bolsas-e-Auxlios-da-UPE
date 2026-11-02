package br.edu.upe.sgba.entity;

import br.edu.upe.sgba.entity.enums.StatusSubmissao;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "submissoes")
@Getter @Setter @NoArgsConstructor
public class Submissao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 10)
    private String protocolo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusSubmissao status;
    @Column(length = 200)
    private String tituloProjeto;
    @Column(columnDefinition = "TEXT")
    private String projetoResumo;
    @Column(columnDefinition = "TEXT")
    private String planoTrabalho;
    private Boolean termoAceite;
    private LocalDateTime dataEnvio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Usuario aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "programa_id", nullable = false)
    private ProgramaBolsa programa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "edital_id", nullable = false)
    private Edital edital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orientador_id")
    private Usuario orientador;

    @Column(length = 100)
    private String cursoMomento;
    private Short periodoMomento;
    @Column(precision = 4, scale = 2)
    private BigDecimal craMomento;
    @Column(length = 50)
    private String unidadeMomento;

    @OneToMany(mappedBy = "submissao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RespostaSubmissao> respostas = new ArrayList<>();

    @OneToMany(mappedBy = "submissao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentoAnexo> documentos = new ArrayList<>();

    @OneToMany(mappedBy = "submissao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HistoricoStatus> historicos = new ArrayList<>();

    @OneToMany(mappedBy = "submissao")
    private List<Notificacao> notificacoes = new ArrayList<>();
}
