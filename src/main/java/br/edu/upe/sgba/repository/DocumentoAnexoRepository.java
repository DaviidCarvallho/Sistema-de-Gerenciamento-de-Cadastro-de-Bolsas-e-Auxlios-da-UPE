package br.edu.upe.sgba.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.upe.sgba.entity.DocumentoAnexo;
import br.edu.upe.sgba.entity.enums.TipoDocumento;

public interface DocumentoAnexoRepository extends JpaRepository<DocumentoAnexo, Long> {

    /** Metadados dos anexos da submissão (RF15) e contagem do painel (RF11). */
    List<DocumentoAnexo> findBySubmissaoId(Long submissaoId);

    /** Localiza documento conferindo a submissão dona (download/delete/RF15). */
    Optional<DocumentoAnexo> findByIdAndSubmissaoId(Long id, Long submissaoId);

    /** RN12 — exigência obrigatória atendida quando há documento do tipo anexado. */
    boolean existsBySubmissaoIdAndTipoDocumento(Long submissaoId, TipoDocumento tipoDocumento);

    /** qtdDocumentos do painel (RF11). */
    int countBySubmissaoId(Long submissaoId);
}
