package br.edu.upe.sgba.dto;

import br.edu.upe.sgba.entity.Usuario;

public record ProfessorResponse(Long id, String nome, String email) {
    public static ProfessorResponse from(Usuario u) {
        return new ProfessorResponse(u.getId(), u.getNome(), u.getEmail());
    }
}