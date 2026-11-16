package br.edu.uniesp.threedcontrol.producao.ordemproducao.application.port;

import java.util.UUID;

/** Porta de saída para notificar o pedido-service sobre o andamento da produção. */
public interface PedidoGateway {
    void notificarInicioProducao(UUID pedidoId);
    void notificarConclusaoProducao(UUID pedidoId);
}
