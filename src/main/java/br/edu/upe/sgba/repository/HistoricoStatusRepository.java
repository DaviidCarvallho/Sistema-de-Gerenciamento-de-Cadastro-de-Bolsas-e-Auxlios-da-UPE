package br.edu.upe.sgba.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.upe.sgba.entity.HistoricoStatus;

public interface HistoricoStatusRepository extends JpaRepository<HistoricoStatus, Long> {

    /** RF12 — timeline append-only, mais recente primeiro (RN07). */
    List<HistoricoStatus> findBySubmissaoIdOrderByDataHoraDesc(Long submissaoId);
}
