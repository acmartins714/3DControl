package br.edu.uniesp.threedcontrol.cadastros.produto.application;

import br.edu.uniesp.threedcontrol.cadastros.produto.domain.ComposicaoProduto;

import java.math.BigDecimal;
import java.util.List;

public record ProdutoCommand(String nome, String descricao, String categoria, String especificacoesTecnicas,
                             int tempoMedioFabricacaoHoras, BigDecimal precoBase,
                             List<ComposicaoProduto> composicao) {
}
