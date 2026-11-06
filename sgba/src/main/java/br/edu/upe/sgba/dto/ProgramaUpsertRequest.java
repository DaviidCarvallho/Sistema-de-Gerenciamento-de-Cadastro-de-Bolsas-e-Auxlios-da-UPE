package br.edu.upe.sgba.dto;

import br.edu.upe.sgba.enums.CategoriaPrograma;
import br.edu.upe.sgba.enums.StatusPrograma;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProgramaUpsertRequest {
    private String titulo;
    private CategoriaPrograma categoria;
    private BigDecimal valorMensal;
    private Short vagas;
    private String periodoLetivo;
    private LocalDateTime dataAbertura;
    private LocalDateTime dataLimite;
    private StatusPrograma status;
    private String descricao;

    public ProgramaUpsertRequest() {
    }

    public CategoriaPrograma getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaPrograma categoria) {
        this.categoria = categoria;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public BigDecimal getValorMensal() {
        return valorMensal;
    }

    public void setValorMensal(BigDecimal valorMensal) {
        this.valorMensal = valorMensal;
    }

    public Short getVagas() {
        return vagas;
    }

    public void setVagas(Short vagas) {
        this.vagas = vagas;
    }

    public String getPeriodoLetivo() {
        return periodoLetivo;
    }

    public void setPeriodoLetivo(String periodoLetivo) {
        this.periodoLetivo = periodoLetivo;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public void setDataAbertura(LocalDateTime dataAbertura) {
        this.dataAbertura = dataAbertura;
    }

    public LocalDateTime getDataLimite() {
        return dataLimite;
    }

    public void setDataLimite(LocalDateTime dataLimite) {
        this.dataLimite = dataLimite;
    }

    public StatusPrograma getStatus() {
        return status;
    }

    public void setStatus(StatusPrograma status) {
        this.status = status;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}