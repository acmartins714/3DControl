package br.edu.uniesp.threedcontrol.pedidos.pedido.application.port;

import java.time.LocalDate;
import java.util.UUID;

/** Porta de saída para o producao-service (fila de produção). */
public interface ProducaoGateway {

    void planejarOrdem(UUID pedidoId, String codigoPedido, LocalDate prazoEntrega, boolean insumosDisponiveis);

    void cancelarOrdem(UUID pedidoId);
}
