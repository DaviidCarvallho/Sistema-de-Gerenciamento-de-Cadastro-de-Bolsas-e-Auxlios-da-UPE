package br.edu.upe.sgba.dto;

import br.edu.upe.sgba.entity.DocumentoAnexo;
import br.edu.upe.sgba.entity.enums.StatusValidacaoDocumento;
import br.edu.upe.sgba.entity.enums.TipoDocumento;

/** Metadados do anexo (upload 201, listagem RF15 e validação RF15). */
public record DocumentoResponse(
        Long id,
        TipoDocumento tipoDocumento,
        String descricaoPersonalizada,
        String nomeOriginal,
        Long tamanhoBytes,
        String mimeType,
        StatusValidacaoDocumento statusValidacao,
        String observacaoValidacao,
        Long submissaoId) {

    public static DocumentoResponse from(DocumentoAnexo d) {
        return new DocumentoResponse(d.getId(), d.getTipoDocumento(), d.getDescricaoPersonalizada(),
                d.getNomeOriginal(), d.getTamanhoBytes(), d.getMimeType(), d.getStatusValidacao(),
                d.getObservacaoValidacao(), d.getSubmissao() == null ? null : d.getSubmissao().getId());
    }
}
