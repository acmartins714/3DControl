package br.edu.uniesp.threedcontrol.cadastros.insumo.infrastructure.persistence;

import br.edu.uniesp.threedcontrol.cadastros.insumo.domain.Insumo;
import br.edu.uniesp.threedcontrol.cadastros.insumo.domain.UnidadeMedida;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "insumo")
public class InsumoJpaEntity {

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

    protected InsumoJpaEntity() {
    }

    static InsumoJpaEntity from(Insumo i) {
        InsumoJpaEntity e = new InsumoJpaEntity();
        e.id = i.getId();
        e.nome = i.getNome();
        e.descricao = i.getDescricao();
        e.unidadeMedida = i.getUnidadeMedida();
        e.custoUnitario = i.getCustoUnitario();
        e.usoRegular = i.isUsoRegular();
        e.ativo = i.isAtivo();
        return e;
    }

    Insumo toDomain() {
        return Insumo.restaurar(id, nome, descricao, unidadeMedida, custoUnitario, usoRegular, ativo);
    }
}
