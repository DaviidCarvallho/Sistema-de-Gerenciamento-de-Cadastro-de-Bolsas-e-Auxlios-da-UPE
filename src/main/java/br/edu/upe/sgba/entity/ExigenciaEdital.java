package br.edu.upe.sgba.entity;

import br.edu.upe.sgba.entity.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exigencias_edital")
@Getter @Setter @NoArgsConstructor
public class ExigenciaEdital {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDocumento tipoDocumento;
    @Column(nullable = false)
    private Boolean obrigatorio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "edital_id", nullable = false)
    private Edital edital;
}
