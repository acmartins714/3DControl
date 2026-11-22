package br.edu.uniesp.threedcontrol.pedidos.pedido;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

    Optional<Pedido> findByCodigo(String codigo);

    List<Pedido> findAllByOrderByPrazoEntrega();

    List<Pedido> findByStatusOrderByPrazoEntrega(StatusPedido status);

    List<Pedido> findByClienteIdOrderByCriadoEmDesc(UUID clienteId);

    List<Pedido> findByStatusAndClienteIdOrderByPrazoEntrega(StatusPedido status, UUID clienteId);
}
