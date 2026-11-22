package br.edu.uniesp.threedcontrol.cadastros.insumo;

import br.edu.uniesp.threedcontrol.cadastros.shared.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Matéria-prima ou componente usado na confecção dos produtos.
 * {@code usoRegular = false} identifica materiais específicos, comprados sob demanda.
 */
@Entity
@Table(name = "insumo")
public class Insumo {

    @Id
    private UUID id;
    private String nome;
    private String descricao;
    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_medida")
    private UnidadeMedida unidadeMedida;
    @Column(name = "custo_unitario")
    private BigDecimal custoUnitario;
    @Column(name = "uso_regular")
    private boolean usoRegular;
    private boolean ativo;

    protected Insumo() {
    }

    public Insumo(String nome, String descricao, UnidadeMedida unidadeMedida,
                  BigDecimal custoUnitario, boolean usoRegular) {
        this.id = UUID.randomUUID();
        this.ativo = true;
        atualizar(nome, descricao, unidadeMedida, custoUnitario, usoRegular);
    }

    public void atualizar(String nome, String descricao, UnidadeMedida unidadeMedida,
                          BigDecimal custoUnitario, boolean usoRegular) {
        this.nome = nome;
        this.descricao = descricao;
        this.unidadeMedida = unidadeMedida;
        this.custoUnitario = custoUnitario;
        this.usoRegular = usoRegular;
        validar();
    }

    public void inativar() {
        this.ativo = false;
    }

    private void validar() {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("O nome do insumo é obrigatório.");
        }
        if (unidadeMedida == null) {
            throw new RegraNegocioException("A unidade de medida do insumo é obrigatória.");
        }
        if (custoUnitario == null || custoUnitario.signum() < 0) {
            throw new RegraNegocioException("O custo unitário do insumo não pode ser negativo.");
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
