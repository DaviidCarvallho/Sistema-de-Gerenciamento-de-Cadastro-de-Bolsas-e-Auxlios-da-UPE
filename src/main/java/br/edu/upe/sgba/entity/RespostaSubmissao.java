
package br.edu.upe.sgba.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "respostas_submissao",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_resposta_unica",
                        columnNames = {"submissao_id", "pergunta_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RespostaSubmissao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String respostaTexto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submissao_id", nullable = false)
    private Submissao submissao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pergunta_id", nullable = false)
    private PerguntaEdital pergunta;
}
