package br.edu.upe.sgba.service;

import br.edu.upe.sgba.dto.AlunoResponse;
import br.edu.upe.sgba.dto.AlunoUpsertRequest;
import br.edu.upe.sgba.dto.EnderecoDto;
import br.edu.upe.sgba.entity.Aluno;
import br.edu.upe.sgba.entity.Usuario;
import br.edu.upe.sgba.entity.enums.FuncaoUsuario;
import br.edu.upe.sgba.exception.RecursoNaoEncontradoException;
import br.edu.upe.sgba.exception.RegraNegocioException;
import br.edu.upe.sgba.repository.AlunoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class AlunoService {

    private final AlunoRepository alunoRepository;
    private final UsuarioService usuarioService;

    @Transactional
    public AlunoResponse criar(AlunoUpsertRequest req) {
        if (req.usuarioId() == null) {
            throw Validacao.argumentoInvalido("O campo 'usuarioId' é obrigatório");
        }
        Usuario usuario = usuarioService.exigeFuncao(req.usuarioId(), FuncaoUsuario.ALUNO); // 404 / 400
        if (alunoRepository.existsById(usuario.getId())) {
            throw new RegraNegocioException("ALUNO_JA_EXISTENTE", "Este usuário já possui perfil de aluno");
        }
        // obrigatórios do DER (colunas not null)
        Validacao.obrigatorio(req.cpf(), "cpf", 14);
        Validacao.obrigatorio(req.matricula(), "matricula", 20);
        Validacao.obrigatorio(req.curso(), "curso", 100);

        String cpf = normalizarCpf(req.cpf());
        String matricula = req.matricula().trim();
        if (alunoRepository.existsByCpf(cpf)) {
            throw new RegraNegocioException("CPF_JA_CADASTRADO", "Já existe aluno com este CPF");
        }
        if (alunoRepository.existsByMatricula(matricula)) {
            throw new RegraNegocioException("MATRICULA_JA_CADASTRADA", "Já existe aluno com esta matrícula");
        }

        Aluno a = new Aluno();
        a.setUsuario(usuario);            // @MapsId: o id vem do usuário
        aplicar(a, req);
        return AlunoResponse.from(alunoRepository.save(a));
    }

    @Transactional(readOnly = true)
    public AlunoResponse buscar(Long usuarioId) {
        return AlunoResponse.from(buscarEntidade(usuarioId));
    }

    @Transactional
    public AlunoResponse atualizar(Long usuarioId, AlunoUpsertRequest req) {
        Aluno a = buscarEntidade(usuarioId);
        if (req.cpf() != null) {
            String cpf = normalizarCpf(req.cpf());
            if (alunoRepository.existsByCpfAndIdNot(cpf, usuarioId)) {
                throw new RegraNegocioException("CPF_JA_CADASTRADO", "Já existe aluno com este CPF");
            }
        }
        if (req.matricula() != null) {
            String m = Validacao.obrigatorio(req.matricula(), "matricula", 20);
            if (alunoRepository.existsByMatriculaAndIdNot(m, usuarioId)) {
                throw new RegraNegocioException("MATRICULA_JA_CADASTRADA", "Já existe aluno com esta matrícula");
            }
        }
        aplicar(a, req);   // parcial: só altera o que veio no payload
        return AlunoResponse.from(a);
    }

    private Aluno buscarEntidade(Long usuarioId) {
        return alunoRepository.findById(usuarioId)
                .orElseThrow(() -> RecursoNaoEncontradoException.porId("Aluno", usuarioId));
    }

    /** Copia só os campos não nulos, validando os tamanhos das colunas do DER. */
    private void aplicar(Aluno a, AlunoUpsertRequest r) {
        if (r.cpf() != null) a.setCpf(normalizarCpf(r.cpf()));
        if (r.rg() != null) a.setRg(Validacao.opcional(r.rg(), "rg", 20));
        if (r.dataNascimento() != null) a.setDataNascimento(r.dataNascimento());
        if (r.nacionalidade() != null) a.setNacionalidade(Validacao.opcional(r.nacionalidade(), "nacionalidade", 50));
        if (r.genero() != null) a.setGenero(r.genero());
        if (r.telefone() != null) a.setTelefone(Validacao.opcional(r.telefone(), "telefone", 20));
        if (r.matricula() != null) a.setMatricula(Validacao.obrigatorio(r.matricula(), "matricula", 20));
        if (r.curso() != null) a.setCurso(Validacao.obrigatorio(r.curso(), "curso", 100));
        if (r.periodoAtual() != null) a.setPeriodoAtual(r.periodoAtual());
        if (r.craAtual() != null) a.setCraAtual(validarCra(r.craAtual()));
        if (r.unidadeAcademica() != null) a.setUnidadeAcademica(Validacao.opcional(r.unidadeAcademica(), "unidadeAcademica", 50));
        if (r.semestreIngresso() != null) a.setSemestreIngresso(Validacao.opcional(r.semestreIngresso(), "semestreIngresso", 10));
        EnderecoDto e = r.endereco();
        if (e != null) {
            if (e.logradouro() != null) a.setLogradouro(Validacao.opcional(e.logradouro(), "logradouro", 150));
            if (e.numero() != null) a.setNumero(Validacao.opcional(e.numero(), "numero", 20));
            if (e.complemento() != null) a.setComplemento(Validacao.opcional(e.complemento(), "complemento", 100));
            if (e.bairro() != null) a.setBairro(Validacao.opcional(e.bairro(), "bairro", 100));
            if (e.cidade() != null) a.setCidade(Validacao.opcional(e.cidade(), "cidade", 100));
            if (e.uf() != null) a.setUf(validarUf(e.uf()));
            if (e.cep() != null) a.setCep(validarCep(e.cep()));
        }
    }

    private String normalizarCpf(String bruto) {
        String cpf = bruto.replaceAll("\\D", "");
        if (cpf.length() != 11) {
            throw Validacao.argumentoInvalido("O CPF deve ter 11 dígitos");
        }
        return cpf;
    }

    private String validarCep(String bruto) {
        String cep = bruto.replaceAll("\\D", "");
        if (cep.length() != 8) {
            throw Validacao.argumentoInvalido("O CEP deve ter 8 dígitos");
        }
        return cep;
    }

    private String validarUf(String bruto) {
        String uf = bruto.trim().toUpperCase();
        if (!uf.matches("[A-Z]{2}")) {
            throw Validacao.argumentoInvalido("A UF deve ter 2 letras");
        }
        return uf;
    }

    private BigDecimal validarCra(BigDecimal cra) {
        if (cra.compareTo(BigDecimal.ZERO) < 0 || cra.compareTo(BigDecimal.TEN) > 0) {
            throw Validacao.argumentoInvalido("O CRA deve estar entre 0 e 10");
        }
        return cra.setScale(2, RoundingMode.HALF_UP);
    }
}