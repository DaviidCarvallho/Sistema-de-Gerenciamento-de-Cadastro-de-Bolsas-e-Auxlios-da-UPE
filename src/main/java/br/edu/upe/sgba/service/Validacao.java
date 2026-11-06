package br.edu.upe.sgba.service;

import org.springframework.http.HttpStatus;

import br.edu.upe.sgba.exception.RegraNegocioException;

final class Validacao {

    private Validacao() {}

    static RegraNegocioException argumentoInvalido(String mensagem) {
        return new RegraNegocioException(HttpStatus.BAD_REQUEST, "ARGUMENTO_INVALIDO", mensagem);
    }

    static String obrigatorio(String valor, String campo, int max) {
        if (valor == null || valor.isBlank()) {
            throw argumentoInvalido("O campo '" + campo + "' é obrigatório");
        }
        return opcional(valor, campo, max);
    }

    static String opcional(String valor, String campo, int max) {
        if (valor == null) return null;
        String v = valor.trim();
        if (v.length() > max) {
            throw argumentoInvalido("O campo '" + campo + "' excede " + max + " caracteres");
        }
        return v;
    }
}