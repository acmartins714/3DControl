package br.edu.uniesp.threedcontrol.pedidos.pedido.domain;

import br.edu.uniesp.threedcontrol.pedidos.shared.domain.DomainException;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Item do pedido. Preço, nome e custo são "fotografados" no momento do pedido para que mudanças
 * posteriores no catálogo não alterem pedidos já fechados.
 */
public record ItemPedido(UUID produtoId, String nomeProduto, String categoria, int quantidade,
                         BigDecimal precoUnitario, BigDecimal custoUnitarioEstimado, String personalizacao) {

    public ItemPedido {
        if (produtoId == null) {
            throw new DomainException("O produto do item é obrigatório.");
        }
        if (quantidade <= 0) {
            throw new DomainException("A quantidade do item deve ser maior que zero.");
        }
        if (precoUnitario == null || precoUnitario.signum() < 0) {
            throw new DomainException("O preço unitário não pode ser negativo.");
        }
        if (custoUnitarioEstimado == null) {
            custoUnitarioEstimado = BigDecimal.ZERO;
        }
    }

    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal custoTotalEstimado() {
        return custoUnitarioEstimado.multiply(BigDecimal.valueOf(quantidade));
    }
}
