package br.edu.uniesp.threedcontrol.pedidos.pedido.application;

import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.Enums.CanalAtendimento;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.Enums.CondicaoPagamento;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.Enums.ModalidadeEntrega;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Dados de entrada dos casos de uso de pedido. */
public final class PedidoCommands {

    private PedidoCommands() {
    }

    /** {@code precoUnitario} é opcional: se ausente, usa o preço base do catálogo. */
    public record ItemCommand(UUID produtoId, int quantidade, BigDecimal precoUnitario, String personalizacao) {
    }

    public record NovoPedido(UUID clienteId, CanalAtendimento canalOrigem, String atendenteResponsavel,
                             LocalDate prazoEntrega, ModalidadeEntrega modalidadeEntrega,
                             CondicaoPagamento condicaoPagamento, String observacoes, List<ItemCommand> itens) {
    }

    public record AlteracaoPedido(LocalDate prazoEntrega, ModalidadeEntrega modalidadeEntrega,
                                  CondicaoPagamento condicaoPagamento, String observacoes, List<ItemCommand> itens) {
    }
}
