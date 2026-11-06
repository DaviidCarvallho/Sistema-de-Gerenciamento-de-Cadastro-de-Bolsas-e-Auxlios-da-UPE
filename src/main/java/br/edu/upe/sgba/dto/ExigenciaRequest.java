package br.edu.upe.sgba.dto;

import br.edu.upe.sgba.entity.enums.TipoDocumento;

public record ExigenciaRequest(TipoDocumento tipoDocumento, Boolean obrigatorio) {}