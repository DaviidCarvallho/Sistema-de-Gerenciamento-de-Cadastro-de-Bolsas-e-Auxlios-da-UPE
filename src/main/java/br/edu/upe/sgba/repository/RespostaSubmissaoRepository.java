package br.edu.upe.sgba.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.upe.sgba.entity.RespostaSubmissao;

public interface RespostaSubmissaoRepository extends JpaRepository<RespostaSubmissao, Long> {

    /** Respostas do rascunho (GET detalhe do wizard RF07). */
    List<RespostaSubmissao> findBySubmissaoId(Long submissaoId);

    /** Upsert por (submissão, pergunta) — uk_resposta_unica (RN03/RN12). */
    Optional<RespostaSubmissao> findBySubmissaoIdAndPerguntaId(Long submissaoId, Long perguntaId);
}
