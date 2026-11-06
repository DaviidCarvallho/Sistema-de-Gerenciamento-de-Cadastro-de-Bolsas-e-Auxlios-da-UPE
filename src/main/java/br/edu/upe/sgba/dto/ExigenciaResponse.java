package br.edu.upe.sgba.dto;

import br.edu.upe.sgba.entity.ExigenciaEdital;
import br.edu.upe.sgba.entity.enums.TipoDocumento;

public record ExigenciaResponse(Long id, TipoDocumento tipoDocumento, Boolean obrigatorio) {
    public static ExigenciaResponse from(ExigenciaEdital x) {
        return new ExigenciaResponse(x.getId(), x.getTipoDocumento(), x.getObrigatorio());
    }
}