package br.edu.upe.sgba.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.edu.upe.sgba.entity.Aluno;
import br.edu.upe.sgba.entity.enums.Genero;

public record AlunoResponse(
        Long usuarioId, String cpf, String rg, LocalDate dataNascimento, String nacionalidade,
        Genero genero, String telefone, String matricula, String curso, Short periodoAtual,
        BigDecimal craAtual, String unidadeAcademica, String semestreIngresso, EnderecoDto endereco) {

    public static AlunoResponse from(Aluno a) {
        return new AlunoResponse(a.getId(), a.getCpf(), a.getRg(), a.getDataNascimento(),
                a.getNacionalidade(), a.getGenero(), a.getTelefone(), a.getMatricula(), a.getCurso(),
                a.getPeriodoAtual(), a.getCraAtual(), a.getUnidadeAcademica(), a.getSemestreIngresso(),
                new EnderecoDto(a.getLogradouro(), a.getNumero(), a.getComplemento(), a.getBairro(),
                        a.getCidade(), a.getUf(), a.getCep()));
    }
}