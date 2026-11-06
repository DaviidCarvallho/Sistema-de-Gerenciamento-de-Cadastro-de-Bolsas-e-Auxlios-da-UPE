package br.edu.upe.sgba.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.edu.upe.sgba.entity.enums.Genero;

public record AlunoUpsertRequest(
        Long usuarioId,            // obrigatório no POST; ignorado no PUT
        String cpf,
        String rg,
        LocalDate dataNascimento,
        String nacionalidade,
        Genero genero,
        String telefone,
        String matricula,
        String curso,
        Short periodoAtual,
        BigDecimal craAtual,
        String unidadeAcademica,
        String semestreIngresso,
        EnderecoDto endereco) {}