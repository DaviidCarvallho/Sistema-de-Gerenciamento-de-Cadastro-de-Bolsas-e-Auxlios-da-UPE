package br.edu.upe.sgba.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import br.edu.upe.sgba.exception.RecursoNaoEncontradoException;
import br.edu.upe.sgba.exception.RegraNegocioException;

@Service
public class ArmazenamentoService {

    private final Path raiz;
    private final long maxBytes;

    public ArmazenamentoService(@Value("${sgba.storage.dir:./storage}") String dir,
                                @Value("${sgba.storage.edital-max-bytes:10485760}") long maxBytes) {
        this.raiz = Path.of(dir).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    /** Salva o PDF oficial do edital e devolve o caminho relativo à raiz de armazenamento. */
    public String salvarPdfEdital(Long editalId, MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "ARQUIVO_INVALIDO",
                    "Envie o arquivo no campo 'file' (multipart/form-data)");
        }
        String tipo = arquivo.getContentType() == null ? "" : arquivo.getContentType().split(";")[0].trim();
        if (!tipo.equalsIgnoreCase("application/pdf")) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "ARQUIVO_INVALIDO",
                    "Apenas arquivos PDF (application/pdf) são aceitos");
        }
        if (arquivo.getSize() > maxBytes) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "ARQUIVO_MUITO_GRANDE",
                    "O arquivo excede o tamanho máximo de " + (maxBytes / (1024 * 1024)) + " MB");
        }

        String relativo = "editais/" + editalId + "/edital-" + editalId + ".pdf";
        Path destino = raiz.resolve(relativo).normalize();
        if (!destino.startsWith(raiz)) {
            throw new IllegalStateException("Caminho de armazenamento inválido");
        }
        try {
            Files.createDirectories(destino.getParent());
            try (InputStream in = arquivo.getInputStream()) {
                Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gravar o arquivo do edital", e);
        }
        return relativo;
    }

    public byte[] ler(String caminhoRelativo) {
        Path origem = raiz.resolve(caminhoRelativo).normalize();
        if (!origem.startsWith(raiz) || !Files.isRegularFile(origem)) {
            throw new RecursoNaoEncontradoException("Arquivo do edital não encontrado no armazenamento");
        }
        try {
            return Files.readAllBytes(origem);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler o arquivo do edital", e);
        }
    }
}