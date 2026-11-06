package br.edu.upe.sgba.repository;

import br.edu.upe.sgba.entity.ProgramaBolsa;
import br.edu.upe.sgba.enums.CategoriaPrograma;
import br.edu.upe.sgba.enums.StatusPrograma;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository

public interface ProgramaBolsaRepository extends JpaRepository<ProgramaBolsa, Long>,JpaSpecificationExecutor<ProgramaBolsa>{

    List<ProgramaBolsa> findByCategoria(CategoriaPrograma categoria);

    List<ProgramaBolsa> findByTituloContainingIgnoreCase(String titulo);

    List<ProgramaBolsa>
    findByStatusAndDataAberturaLessThanEqualAndDataLimiteGreaterThanEqual(StatusPrograma status, LocalDateTime dataAbertura, LocalDateTime dataLimite);

    Page<ProgramaBolsa> findAll(Pageable pageable);
}
