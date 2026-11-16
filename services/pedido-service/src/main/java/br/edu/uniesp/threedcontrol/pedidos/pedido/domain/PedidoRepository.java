package br.edu.uniesp.threedcontrol.pedidos.pedido.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PedidoRepository {
    Pedido salvar(Pedido pedido);
    Optional<Pedido> buscarPorId(UUID id);
    Optional<Pedido> buscarPorCodigo(String codigo);
    List<Pedido> listar(StatusPedido status, UUID clienteId);
}
