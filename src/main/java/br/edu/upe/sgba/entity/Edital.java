
package br.edu.upe.sgba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "editais",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_edital_numero_ano",
                        columnNames = {"numero_edital", "ano"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Edital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_edital", nullable = false, length = 30)
    private String numeroEdital;

    @Column(nullable = false)
    private Short ano;

    @Column(nullable = false)
    private LocalDate dataPublicacao;

    @Column(nullable = false)
    private LocalDateTime dataInicioSubmissoes;

    @Column(nullable = false)
    private LocalDateTime dataFimSubmissoes;

    @Column(length = 500)
    private String caminhoArquivoPdf;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "programa_id", nullable = false)
    private ProgramaBolsa programa;

    @OneToMany(
            mappedBy = "edital",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ExigenciaEdital> exigencias = new ArrayList<>();

    @OneToMany(
            mappedBy = "edital",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<PerguntaEdital> perguntas = new ArrayList<>();
}
