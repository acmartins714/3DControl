package br.edu.uniesp.threedcontrol.cadastros.insumo.domain;

import br.edu.uniesp.threedcontrol.cadastros.shared.domain.DomainException;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Matéria-prima ou componente usado na confecção dos produtos.
 * {@code usoRegular = false} identifica materiais específicos, comprados sob demanda.
 */
public class Insumo {

    private final UUID id;
    private String nome;
    private String descricao;
    private UnidadeMedida unidadeMedida;
    private BigDecimal custoUnitario;
    private boolean usoRegular;
    private boolean ativo;

    private Insumo(UUID id, String nome, String descricao, UnidadeMedida unidadeMedida,
                   BigDecimal custoUnitario, boolean usoRegular, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.unidadeMedida = unidadeMedida;
        this.custoUnitario = custoUnitario;
        this.usoRegular = usoRegular;
        this.ativo = ativo;
    }

    public static Insumo novo(String nome, String descricao, UnidadeMedida unidade,
                              BigDecimal custoUnitario, boolean usoRegular) {
        Insumo i = new Insumo(UUID.randomUUID(), nome, descricao, unidade, custoUnitario, usoRegular, true);
        i.validar();
        return i;
    }

    public static Insumo restaurar(UUID id, String nome, String descricao, UnidadeMedida unidade,
                                   BigDecimal custoUnitario, boolean usoRegular, boolean ativo) {
        return new Insumo(id, nome, descricao, unidade, custoUnitario, usoRegular, ativo);
    }

    public void atualizar(String nome, String descricao, UnidadeMedida unidade,
                          BigDecimal custoUnitario, boolean usoRegular) {
        this.nome = nome;
        this.descricao = descricao;
        this.unidadeMedida = unidade;
        this.custoUnitario = custoUnitario;
        this.usoRegular = usoRegular;
        validar();
    }

    public void inativar() {
        this.ativo = false;
    }

    private void validar() {
        if (nome == null || nome.isBlank()) {
            throw new DomainException("O nome do insumo é obrigatório.");
        }
        if (unidadeMedida == null) {
            throw new DomainException("A unidade de medida do insumo é obrigatória.");
        }
        if (custoUnitario == null || custoUnitario.signum() < 0) {
            throw new DomainException("O custo unitário do insumo não pode ser negativo.");
        }
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public UnidadeMedida getUnidadeMedida() { return unidadeMedida; }
    public BigDecimal getCustoUnitario() { return custoUnitario; }
    public boolean isUsoRegular() { return usoRegular; }
    public boolean isAtivo() { return ativo; }
}
