package br.edu.upe.sgba.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.upe.sgba.entity.Submissao;
import br.edu.upe.sgba.entity.enums.StatusSubmissao;

public interface SubmissaoRepository extends JpaRepository<Submissao, Long> {

    /** Painel do aluno (RF11): submissões do usuário corrente (D1), mais recentes primeiro. */
    List<Submissao> findByAlunoIdOrderByCriadoEmDesc(Long alunoId);

    /** RN08 — unicidade de rascunho por (aluno, edital) (uk_aluno_edital). */
    boolean existsByAlunoIdAndEditalId(Long alunoId, Long editalId);

    /** Consulta por protocolo gerado no envio (RN02). */
    Optional<Submissao> findByProtocolo(String protocolo);

    /** Pré-checagem do gerador de protocolo (D6 — loop de 5 tentativas). */
    boolean existsByProtocolo(String protocolo);

    Page<Submissao> findByEditalId(Long editalId, Pageable pageable);

    /** RF14 — painel do gerenciador com filtros combináveis por status/edital/programa (D4). */
    @Query("""
            select s from Submissao s
            where (:status is null or s.status = :status)
              and (:editalId is null or s.edital.id = :editalId)
              and (:programaId is null or s.edital.programa.id = :programaId)
            """)
    Page<Submissao> filtrar(@Param("status") StatusSubmissao status,
                            @Param("editalId") Long editalId,
                            @Param("programaId") Long programaId,
                            Pageable pageable);
}
