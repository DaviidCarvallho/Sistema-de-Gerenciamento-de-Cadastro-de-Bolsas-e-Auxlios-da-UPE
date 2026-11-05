
package br.edu.upe.sgba.entity;

import br.edu.upe.sgba.entity.enums.Genero;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "alunos")
@Getter
@Setter
@NoArgsConstructor
public class Aluno {

    @Id
    @Column(name = "usuario_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(length = 20)
    private String rg;

    private LocalDate dataNascimento;

    @Column(length = 50)
    private String nacionalidade;

    @Enumerated(EnumType.STRING)
    private Genero genero;

    @Column(length = 20)
    private String telefone;

    @Column(nullable = false, unique = true, length = 20)
    private String matricula;

    @Column(length = 100)
    private String curso;

    private Short periodoAtual;

    @Column(precision = 4, scale = 2)
    private BigDecimal craAtual;

    @Column(length = 50)
    private String unidadeAcademica;

    @Column(length = 10)
    private String semestreIngresso;

    @Column(length = 150)
    private String logradouro;

    @Column(length = 20)
    private String numero;

    @Column(length = 100)
    private String complemento;

    @Column(length = 100)
    private String bairro;

    @Column(length = 100)
    private String cidade;

    @Column(length = 2)
    private String uf;

    @Column(length = 8)
    private String cep;
}
