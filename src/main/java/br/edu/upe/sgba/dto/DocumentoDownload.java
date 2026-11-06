package br.edu.upe.sgba.dto;

/** Download do documento: bytes + nome original do arquivo (Content-Disposition). */
public record DocumentoDownload(byte[] conteudo, String nomeOriginal) {
}
