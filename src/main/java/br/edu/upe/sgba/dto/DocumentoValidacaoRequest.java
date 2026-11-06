package br.edu.upe.sgba.dto;

import br.edu.upe.sgba.entity.enums.StatusValidacaoDocumento;

/** RF15 — validação do anexo pelo gerenciador; observação obrigatória para INCOMPLETO/VENCIDO. */
public record DocumentoValidacaoRequest(StatusValidacaoDocumento statusValidacao, String observacaoValidacao) {
}
