package br.edu.upe.sgba.dto;

import java.time.LocalDateTime;

import br.edu.upe.sgba.entity.Usuario;
import br.edu.upe.sgba.entity.enums.FuncaoUsuario;

public record UsuarioResponse(Long id, String nome, String email, FuncaoUsuario funcao,
                              LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getFuncao(),
                u.getCriadoEm(), u.getAtualizadoEm());
    }
}