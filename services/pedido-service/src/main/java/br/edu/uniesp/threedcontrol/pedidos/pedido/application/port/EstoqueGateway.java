package br.edu.uniesp.threedcontrol.pedidos.pedido.application.port;

import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.CadastroGateway.InsumoNecessario;

import java.util.List;
import java.util.UUID;

/** Porta de saída para o estoque-service. */
public interface EstoqueGateway {

    record ResultadoReserva(boolean reservado, List<String> insumosFaltantes) {
    }

    /** Reserva (ou re-reserva, de forma idempotente) os insumos do pedido. */
    ResultadoReserva reservar(UUID pedidoId, List<InsumoNecessario> necessidades);

    void consumirReservas(UUID pedidoId);

    void liberarReservas(UUID pedidoId);
}
