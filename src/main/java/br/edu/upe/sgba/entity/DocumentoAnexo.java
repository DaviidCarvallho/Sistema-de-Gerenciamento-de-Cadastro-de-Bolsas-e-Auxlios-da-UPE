package br.edu.upe.sgba.entity;

import br.edu.upe.sgba.entity.enums.TipoDocumento;
import br.edu.upe.sgba.entity.enums.StatusValidacaoDocumento;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "documentos_anexo")
@Getter @Setter @NoArgsConstructor
public class DocumentoAnexo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDocumento tipoDocumento;
    @Column(length = 255)
    private String descricaoPersonalizada;
    @Column(nullable = false, length = 255)
    private String nomeOriginal;
    @Column(nullable = false, length = 500)
    private String caminhoArquivo;
    private Long tamanhoBytes;
    @Column(length = 100, nullable = false)
    private String mimeType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusValidacaoDocumento statusValidacao;
    @Column(length = 500)
    private String observacaoValidacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submissao_id", nullable = false)
    private Submissao submissao;
}
