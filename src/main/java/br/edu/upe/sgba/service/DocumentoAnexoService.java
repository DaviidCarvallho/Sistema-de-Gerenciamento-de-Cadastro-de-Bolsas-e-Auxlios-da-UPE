package br.edu.upe.sgba.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.edu.upe.sgba.dto.DocumentoDownload;
import br.edu.upe.sgba.dto.DocumentoResponse;
import br.edu.upe.sgba.dto.DocumentoValidacaoRequest;
import br.edu.upe.sgba.entity.DocumentoAnexo;
import br.edu.upe.sgba.entity.Submissao;
import br.edu.upe.sgba.entity.Usuario;
import br.edu.upe.sgba.entity.enums.FuncaoUsuario;
import br.edu.upe.sgba.entity.enums.StatusSubmissao;
import br.edu.upe.sgba.entity.enums.StatusValidacaoDocumento;
import br.edu.upe.sgba.entity.enums.TipoDocumento;
import br.edu.upe.sgba.exception.RecursoNaoEncontradoException;
import br.edu.upe.sgba.exception.RegraNegocioException;
import br.edu.upe.sgba.repository.DocumentoAnexoRepository;
import br.edu.upe.sgba.repository.SubmissaoRepository;
import lombok.RequiredArgsConstructor;

/**
 * Anexos de documentos da submissão (Issue 6 — RF09/RF15/RN03/RN11/D7).
 *
 * <p>Escritas (upload/delete) são do <b>dono</b> (RF09); consultas/download e a
 * validação do anexo (RF15) seguem RN06/§9.2. Arquivos em disco via
 * {@link ArmazenamentoService} (D7).</p>
 */
@Service
@RequiredArgsConstructor
public class DocumentoAnexoService {

    private final DocumentoAnexoRepository documentosRepository;
    private final SubmissaoRepository submissoesRepository;
    private final UsuarioService usuarios;
    private final ArmazenamentoService armazenamento;

    /** RF09 — upload multipart; só RASCUNHO/PENDENTE_DOCUMENTOS (RN03); OUTRO exige descrição (RN11). */
    @Transactional
    public DocumentoResponse anexar(Long submissaoId, Long usuarioId, MultipartFile arquivo,
            String tipoDocumento, String descricaoPersonalizada) {
        Submissao submissao = buscarSubmissao(submissaoId);
        exigirUsuarioId(usuarioId);
        if (!submissao.getAluno().getId().equals(usuarioId)) {
            throw RecursoNaoEncontradoException.porId("Submissao", submissaoId);
        }
        if (submissao.getStatus() != StatusSubmissao.RASCUNHO
                && submissao.getStatus() != StatusSubmissao.PENDENTE_DOCUMENTOS) {   // RN03
            throw new RegraNegocioException("STATUS_NAO_EDITAVEL",
                    "Documentos só podem ser anexados em RASCUNHO ou PENDENTE_DOCUMENTOS (status atual: "
                            + submissao.getStatus() + ")");
        }
        TipoDocumento tipo;
        try {
            tipo = TipoDocumento.valueOf(tipoDocumento);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw Validacao.argumentoInvalido("Informe 'tipoDocumento' válido (enum do edital)");
        }
        if (tipo == TipoDocumento.OUTRO
                && (descricaoPersonalizada == null || descricaoPersonalizada.isBlank())) {   // RN11
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "DESCRICAO_OBRIGATORIA",
                    "Informe 'descricaoPersonalizada' quando tipoDocumento = OUTRO");
        }

        String caminho = armazenamento.salvarDocumento(submissao.getId(), arquivo);
        DocumentoAnexo documento = new DocumentoAnexo();
        documento.setSubmissao(submissao);
        documento.setTipoDocumento(tipo);
        documento.setDescricaoPersonalizada(Validacao.opcional(descricaoPersonalizada, "descricaoPersonalizada", 255));
        documento.setNomeOriginal(arquivo.getOriginalFilename() == null ? "documento.pdf"
                : arquivo.getOriginalFilename());
        documento.setCaminhoArquivo(caminho);
        documento.setTamanhoBytes(arquivo.getSize());
        documento.setMimeType(arquivo.getContentType() == null ? "application/pdf"
                : arquivo.getContentType().split(";")[0].trim());
        documento.setStatusValidacao(StatusValidacaoDocumento.PENDENTE);
        return DocumentoResponse.from(documentosRepository.save(documento));
    }

    /** Metadados dos documentos da submissão (RF15) — dono OU gerenciador (RN06). */
    @Transactional(readOnly = true)
    public List<DocumentoResponse> listar(Long submissaoId, Long usuarioId) {
        Submissao submissao = buscarSubmissao(submissaoId);
        assegurarVisibilidade(submissao, usuarioId);
        return documentosRepository.findBySubmissaoId(submissaoId).stream()
                .map(DocumentoResponse::from)
                .toList();
    }

    /** Download com os mesmos bytes do upload (RF09) — dono OU gerenciador (RN06). */
    @Transactional
    public DocumentoDownload baixar(Long documentoId, Long usuarioId) {
        DocumentoAnexo documento = buscaDocumentoComVisibilidade(documentoId, usuarioId);
        byte[] conteudo = armazenamento.ler(documento.getCaminhoArquivo());
        return new DocumentoDownload(conteudo, documento.getNomeOriginal());
    }

    /** RN03 — exclusão apenas em RASCUNHO/PENDENTE_DOCUMENTOS e pelo dono (RF09). */
    @Transactional
    public void excluir(Long documentoId, Long usuarioId) {
        DocumentoAnexo documento = documentosRepository.findById(documentoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.porId("Documento", documentoId));
        Submissao submissao = documento.getSubmissao();
        exigirUsuarioId(usuarioId);
        if (!submissao.getAluno().getId().equals(usuarioId)) {                       // escrita = dono
            throw RecursoNaoEncontradoException.porId("Documento", documentoId);
        }
        if (submissao.getStatus() != StatusSubmissao.RASCUNHO
                && submissao.getStatus() != StatusSubmissao.PENDENTE_DOCUMENTOS) {   // RN03
            throw new RegraNegocioException("STATUS_NAO_EDITAVEL",
                    "Documentos só podem ser excluídos em RASCUNHO ou PENDENTE_DOCUMENTOS (status atual: "
                            + submissao.getStatus() + ")");
        }
        String caminho = documento.getCaminhoArquivo();
        documentosRepository.delete(documento);
        armazenamento.excluir(caminho);
    }

    /** RF15 — validação do anexo pelo gerenciador (§9.2): 400 função/enum/observação. */
    @Transactional
    public DocumentoResponse validarStatus(Long documentoId, Long gerenciadorId, DocumentoValidacaoRequest req) {
        DocumentoAnexo documento = documentosRepository.findById(documentoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.porId("Documento", documentoId));
        usuarios.exigeFuncao(gerenciadorId, FuncaoUsuario.GERENCIADOR);               // §9.2
        if (req == null || req.statusValidacao() == null) {
            throw Validacao.argumentoInvalido("Informe 'statusValidacao'");
        }
        boolean exigeObservacao = (req.statusValidacao() == StatusValidacaoDocumento.INCOMPLETO
                || req.statusValidacao() == StatusValidacaoDocumento.VENCIDO);
        if (exigeObservacao && (req.observacaoValidacao() == null || req.observacaoValidacao().isBlank())) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "OBSERVACAO_OBRIGATORIA",
                    "A observação é obrigatória para os status INCOMPLETO e VENCIDO");
        }
        documento.setStatusValidacao(req.statusValidacao());
        documento.setObservacaoValidacao(Validacao.opcional(req.observacaoValidacao(), "observacaoValidacao", 500));
        return DocumentoResponse.from(documentosRepository.save(documento));
    }

    // ------------------------------ helpers ------------------------------

    private Submissao buscarSubmissao(Long submissaoId) {
        return submissoesRepository.findById(submissaoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.porId("Submissao", submissaoId));
    }

    private DocumentoAnexo buscaDocumentoComVisibilidade(Long documentoId, Long usuarioId) {
        DocumentoAnexo documento = documentosRepository.findById(documentoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.porId("Documento", documentoId));
        assegurarVisibilidade(documento.getSubmissao(), usuarioId);
        return documento;
    }

    private void exigirUsuarioId(Long usuarioId) {
        if (usuarioId == null) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "ARGUMENTO_INVALIDO",
                    "Usuário não informado (header X-Usuario-Id)");
        }
    }

    /** RN06 — dono OU GERENCIADOR; caso contrário 404 sem vazar existência. */
    private void assegurarVisibilidade(Submissao submissao, Long usuarioId) {
        exigirUsuarioId(usuarioId);
        if (submissao.getAluno().getId().equals(usuarioId)) {
            return;
        }
        Usuario usuario = usuarios.buscarEntidade(usuarioId);
        if (usuario.getFuncao() == FuncaoUsuario.GERENCIADOR) {
            return;
        }
        throw RecursoNaoEncontradoException.porId("Documento", submissao.getId());
    }
}
