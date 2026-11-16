package br.edu.uniesp.threedcontrol.estoque.estoque.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Quantidade total de um insumo exigida por um pedido. */
public record NecessidadeInsumo(UUID insumoId, BigDecimal quantidade) {
}
