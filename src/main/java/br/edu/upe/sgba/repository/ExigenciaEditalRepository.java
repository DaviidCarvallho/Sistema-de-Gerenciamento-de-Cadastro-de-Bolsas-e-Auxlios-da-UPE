package br.edu.upe.sgba.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.upe.sgba.entity.ExigenciaEdital;
import br.edu.upe.sgba.entity.enums.TipoDocumento;

public interface ExigenciaEditalRepository extends JpaRepository<ExigenciaEdital, Long> {
    List<ExigenciaEdital> findByEditalIdOrderByIdAsc(Long editalId);
    Optional<ExigenciaEdital> findByIdAndEditalId(Long id, Long editalId);
    boolean existsByEditalIdAndTipoDocumento(Long editalId, TipoDocumento tipoDocumento);
    boolean existsByEditalIdAndTipoDocumentoAndIdNot(Long editalId, TipoDocumento tipoDocumento, Long id);
}