package br.edu.uniesp.threedcontrol.pedidos.pedido;

import br.edu.uniesp.threedcontrol.pedidos.shared.RegraNegocioException;

/** Lançada quando se tenta alterar um pedido que já entrou em produção (Regra de Ouro). */
public class PedidoNaoAlteravelException extends RegraNegocioException {

    public PedidoNaoAlteravelException(String codigo, StatusPedido status) {
        super("O pedido " + codigo + " não pode mais ser alterado: status atual '" + status.descricao()
                + "'. Alterações só são permitidas antes da entrada em produção.");
    }
}
