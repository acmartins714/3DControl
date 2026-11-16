package br.edu.uniesp.threedcontrol.pedidos.pedido.domain;

import br.edu.uniesp.threedcontrol.pedidos.shared.domain.DomainException;

/** Lançada quando se tenta alterar um pedido que já entrou em produção (Regra de Ouro). */
public class PedidoNaoAlteravelException extends DomainException {
    public PedidoNaoAlteravelException(String codigo, StatusPedido status) {
        super("O pedido " + codigo + " não pode mais ser alterado: status atual '" + status.descricao()
                + "'. Alterações só são permitidas antes da entrada em produção.");
    }
}
