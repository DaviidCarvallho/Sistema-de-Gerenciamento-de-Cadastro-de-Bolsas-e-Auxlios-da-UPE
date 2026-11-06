package br.edu.upe.sgba.repository;

import br.edu.upe.sgba.entity.ProgramaBolsa;
import br.edu.upe.sgba.entity.enums.CategoriaPrograma;
import br.edu.upe.sgba.entity.enums.StatusPrograma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Programas de Bolsa (RF03/RF05/RF14). A paginação com filtros combináveis usa
 * {@link JpaSpecificationExecutor} (cuidado do Service — D4); paging base é
 * herdado de {@link JpaRepository}.
 */
@Repository
public interface ProgramaBolsaRepository extends JpaRepository<ProgramaBolsa, Long>, JpaSpecificationExecutor<ProgramaBolsa> {

    List<ProgramaBolsa> findByCategoria(CategoriaPrograma categoria);

    List<ProgramaBolsa> findByTituloContainingIgnoreCase(String titulo);

    /** Janela efetiva de visibilidade (RF05): ABERTO e agora ∈ [abertura, limite]. */
    List<ProgramaBolsa> findByStatusAndDataAberturaLessThanEqualAndDataLimiteGreaterThanEqual(
            StatusPrograma status, LocalDateTime dataAbertura, LocalDateTime dataLimite);
}
