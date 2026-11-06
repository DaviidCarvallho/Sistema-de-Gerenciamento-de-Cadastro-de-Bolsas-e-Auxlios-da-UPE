package br.edu.upe.sgba.dto;

public record PerguntaRequest(String enunciado, Boolean obrigatoria, Short ordemExibicao) {}