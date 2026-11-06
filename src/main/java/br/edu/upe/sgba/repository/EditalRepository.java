package br.edu.upe.sgba.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.upe.sgba.entity.Edital;

public interface EditalRepository extends JpaRepository<Edital, Long> {
    Page<Edital> findByProgramaId(Long programaId, Pageable pageable);
    Optional<Edital> findByNumeroEditalAndAno(String numeroEdital, Short ano);
    boolean existsByNumeroEditalAndAno(String numeroEdital, Short ano);
    boolean existsByNumeroEditalAndAnoAndIdNot(String numeroEdital, Short ano, Long id);
}