package br.edu.upe.sgba.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.upe.sgba.entity.PerguntaEdital;

public interface PerguntaEditalRepository extends JpaRepository<PerguntaEdital, Long> {
    List<PerguntaEdital> findByEditalIdOrderByOrdemExibicaoAsc(Long editalId);
    Optional<PerguntaEdital> findByIdAndEditalId(Long id, Long editalId);
    boolean existsByEditalIdAndOrdemExibicao(Long editalId, Short ordemExibicao);
    boolean existsByEditalIdAndOrdemExibicaoAndIdNot(Long editalId, Short ordemExibicao, Long id);
}