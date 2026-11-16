package br.edu.uniesp.threedcontrol.cadastros.produto.application;

import br.edu.uniesp.threedcontrol.cadastros.insumo.domain.UnidadeMedida;
import br.edu.uniesp.threedcontrol.cadastros.produto.domain.Produto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Visão de leitura do produto com a ficha técnica resolvida e o custo estimado por unidade. */
public record ProdutoDetalhado(Produto produto, List<InsumoDaComposicao> composicao, BigDecimal custoEstimado) {

    public record InsumoDaComposicao(UUID insumoId, String nome, UnidadeMedida unidadeMedida,
                                     BigDecimal quantidade, BigDecimal custoUnitario) {
    }
}
