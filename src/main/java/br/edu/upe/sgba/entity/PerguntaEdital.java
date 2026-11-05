
package br.edu.upe.sgba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "perguntas_edital",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pergunta_ordem",
                        columnNames = {"edital_id", "ordem_exibicao"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PerguntaEdital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String enunciado;

    @Column(nullable = false)
    private Boolean obrigatoria;

    @Column(name = "ordem_exibicao", nullable = false)
    private Short ordemExibicao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "edital_id", nullable = false)
    private Edital edital;

    @OneToMany(mappedBy = "pergunta")
    private List<RespostaSubmissao> respostas = new ArrayList<>();
}
