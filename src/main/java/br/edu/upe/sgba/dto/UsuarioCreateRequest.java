package br.edu.upe.sgba.dto;

import br.edu.upe.sgba.entity.enums.FuncaoUsuario;

public record UsuarioCreateRequest(String nome, String email, String senha, FuncaoUsuario funcao) {}