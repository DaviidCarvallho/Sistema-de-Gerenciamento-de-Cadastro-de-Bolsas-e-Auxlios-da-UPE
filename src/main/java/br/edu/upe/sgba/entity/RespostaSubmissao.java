package br.edu.upe.sgba.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "respostas_submissao")
@Getter @Setter @NoArgsConstructor
public class RespostaSubmissao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(columnDefinition = "TEXT")
    private String respostaTexto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submissao_id", nullable = false)
    private Submissao submissao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pergunta_id", nullable = false)
    private PerguntaEdital pergunta;
}
