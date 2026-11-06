package br.edu.upe.sgba.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.upe.sgba.entity.Aluno;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {
    boolean existsByCpf(String cpf);
    boolean existsByMatricula(String matricula);
    boolean existsByCpfAndIdNot(String cpf, Long id);
    boolean existsByMatriculaAndIdNot(String matricula, Long id);
}