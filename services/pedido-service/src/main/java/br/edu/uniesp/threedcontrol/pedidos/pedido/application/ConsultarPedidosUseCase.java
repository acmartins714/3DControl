package br.edu.uniesp.threedcontrol.pedidos.pedido.application;

import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.Pedido;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.PedidoRepository;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.StatusPedido;
import br.edu.uniesp.threedcontrol.pedidos.shared.domain.RecursoNaoEncontradoException;

import java.util.List;
import java.util.UUID;

public class ConsultarPedidosUseCase {

    private final PedidoRepository pedidos;

    public ConsultarPedidosUseCase(PedidoRepository pedidos) {
        this.pedidos = pedidos;
    }

    public Pedido buscar(UUID id) {
        return pedidos.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
    }

    public List<Pedido> listar(StatusPedido status, UUID clienteId) {
        return pedidos.listar(status, clienteId);
    }
}
