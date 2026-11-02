package br.edu.upe.sgba.entity;

import br.edu.upe.sgba.entity.enums.CategoriaPrograma;
import br.edu.upe.sgba.entity.enums.StatusPrograma;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "programas_bolsa")
@Getter @Setter @NoArgsConstructor
public class ProgramaBolsa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String titulo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaPrograma categoria;
    @Column(columnDefinition = "TEXT")
    private String descricao;
    @Column(precision = 10, scale = 2)
    private BigDecimal valorMensal;
    private Short vagas;
    @Column(length = 10)
    private String periodoLetivo;
    private LocalDateTime dataAbertura;
    private LocalDateTime dataLimite;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPrograma status;
    private LocalDateTime criadoEm;

    @OneToMany(mappedBy = "programa", cascade = CascadeType.ALL)
    private List<Edital> editais = new ArrayList<>();

    @OneToMany(mappedBy = "programa")
    private List<Submissao> submissoes = new ArrayList<>();
}
