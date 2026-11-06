package br.edu.upe.sgba.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.upe.sgba.dto.ProfessorResponse;
import br.edu.upe.sgba.dto.UsuarioCreateRequest;
import br.edu.upe.sgba.dto.UsuarioResponse;
import br.edu.upe.sgba.dto.UsuarioUpdateRequest;
import br.edu.upe.sgba.entity.Usuario;
import br.edu.upe.sgba.entity.enums.FuncaoUsuario;
import br.edu.upe.sgba.exception.RecursoNaoEncontradoException;
import br.edu.upe.sgba.exception.RegraNegocioException;
import br.edu.upe.sgba.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final String DOMINIO = "@upe.br";

    private final UsuarioRepository usuarioRepository;

    @Transactional
    public UsuarioResponse criar(UsuarioCreateRequest req) {
        String nome = Validacao.obrigatorio(req.nome(), "nome", 150);
        String senha = Validacao.obrigatorio(req.senha(), "senha", 255);
        if (req.funcao() == null) {
            throw Validacao.argumentoInvalido("O campo 'funcao' é obrigatório");
        }
        String email = validarEmail(req.email());               // RN01 → 400
        if (usuarioRepository.existsByEmail(email)) {
            throw emailDuplicado();                              // → 409
        }

        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(email);
        u.setSenhaHash(senha);          // cp3: sem hash (cp4 troca por BCrypt)
        u.setFuncao(req.funcao());
        u.setCriadoEm(LocalDateTime.now());
        return UsuarioResponse.from(usuarioRepository.save(u));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) {
        return UsuarioResponse.from(buscarEntidade(id));
    }

    /** RF07 passo 2 / RN10: lista vazia é 200 com [] (decisão registrada no PR). */
    @Transactional(readOnly = true)
    public List<ProfessorResponse> listarPorFuncao(FuncaoUsuario funcao) {
        return usuarioRepository.findByFuncao(funcao).stream().map(ProfessorResponse::from).toList();
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioUpdateRequest req) {
        Usuario u = buscarEntidade(id);
        if (req.nome() != null) {
            u.setNome(Validacao.obrigatorio(req.nome(), "nome", 150));
        }
        if (req.email() != null) {
            String email = validarEmail(req.email());
            if (usuarioRepository.existsByEmailAndIdNot(email, id)) {
                throw emailDuplicado();
            }
            u.setEmail(email);
        }
        if (req.senha() != null) {
            u.setSenhaHash(Validacao.obrigatorio(req.senha(), "senha", 255));
        }
        u.setAtualizadoEm(LocalDateTime.now());
        return UsuarioResponse.from(u);
    }

    /**
     * Helper das Issues 5 e 6 (decisão §9.2): usuário inexistente → 404;
     * função fora das permitidas → 400 PERMISSAO_INSUFICIENTE.
     */
    @Transactional(readOnly = true)
    public Usuario exigeFuncao(Long usuarioId, FuncaoUsuario... permitidas) {
        if (usuarioId == null) {
            throw Validacao.argumentoInvalido("Usuário não informado (header X-Usuario-Id)");
        }
        Usuario u = buscarEntidade(usuarioId);
        if (!Arrays.asList(permitidas).contains(u.getFuncao())) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "PERMISSAO_INSUFICIENTE",
                    "A função do usuário não permite esta operação");
        }
        return u;
    }

    Usuario buscarEntidade(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.porId("Usuario", id));
    }

    private String validarEmail(String bruto) {
        String email = Validacao.obrigatorio(bruto, "email", 255).toLowerCase();
        boolean terminaNoDominio = email.endsWith(DOMINIO);
        String local = terminaNoDominio ? email.substring(0, email.length() - DOMINIO.length()) : "";
        if (!terminaNoDominio || local.isEmpty() || local.contains("@") || local.contains(" ")) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "EMAIL_INSTITUCIONAL_INVALIDO",
                    "O e-mail deve terminar em @upe.br");
        }
        return email;
    }

    private RegraNegocioException emailDuplicado() {
        return new RegraNegocioException("EMAIL_JA_CADASTRADO", "Já existe usuário com este e-mail");
    }
}