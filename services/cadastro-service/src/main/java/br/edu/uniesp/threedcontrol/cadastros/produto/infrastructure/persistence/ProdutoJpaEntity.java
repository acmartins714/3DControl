package br.edu.uniesp.threedcontrol.cadastros.produto.infrastructure.persistence;

import br.edu.uniesp.threedcontrol.cadastros.produto.domain.ComposicaoProduto;
import br.edu.uniesp.threedcontrol.cadastros.produto.domain.Produto;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "produto")
public class ProdutoJpaEntity {

    @Embeddable
    public static class ComposicaoEmbeddable {
        @Column(name = "insumo_id")
        private UUID insumoId;
        private BigDecimal quantidade;

        protected ComposicaoEmbeddable() {
        }

        ComposicaoEmbeddable(UUID insumoId, BigDecimal quantidade) {
            this.insumoId = insumoId;
            this.quantidade = quantidade;
        }
    }

    @Id
    private UUID id;
    private String nome;
    private String descricao;
    private String categoria;
    @Column(name = "especificacoes_tecnicas")
    private String especificacoesTecnicas;
    @Column(name = "tempo_medio_fabricacao_horas")
    private int tempoMedioFabricacaoHoras;
    @Column(name = "preco_base")
    private BigDecimal precoBase;
    private boolean ativo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "produto_composicao", joinColumns = @JoinColumn(name = "produto_id"))
    private List<ComposicaoEmbeddable> composicao = new ArrayList<>();

    protected ProdutoJpaEntity() {
    }

    static ProdutoJpaEntity from(Produto p) {
        ProdutoJpaEntity e = new ProdutoJpaEntity();
        e.id = p.getId();
        e.nome = p.getNome();
        e.descricao = p.getDescricao();
        e.categoria = p.getCategoria();
        e.especificacoesTecnicas = p.getEspecificacoesTecnicas();
        e.tempoMedioFabricacaoHoras = p.getTempoMedioFabricacaoHoras();
        e.precoBase = p.getPrecoBase();
        e.ativo = p.isAtivo();
        p.getComposicao().forEach(c -> e.composicao.add(new ComposicaoEmbeddable(c.insumoId(), c.quantidade())));
        return e;
    }

    Produto toDomain() {
        List<ComposicaoProduto> itens = composicao.stream()
                .map(c -> new ComposicaoProduto(c.insumoId, c.quantidade)).toList();
        return Produto.restaurar(id, nome, descricao, categoria, especificacoesTecnicas,
                tempoMedioFabricacaoHoras, precoBase, itens, ativo);
    }
}
