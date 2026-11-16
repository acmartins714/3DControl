package br.edu.uniesp.threedcontrol.pedidos.pedido.infrastructure.client;

import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.ProducaoGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.UUID;

@Component
class ProducaoHttpGateway implements ProducaoGateway {

    private static final String SERVICO = "producao-service";

    record PlanejamentoRequest(UUID pedidoId, String codigoPedido, LocalDate prazoEntrega, boolean insumosDisponiveis) {
    }

    private final RestClient rest;

    ProducaoHttpGateway(RestClient.Builder builder, @Value("${integracao.producao-url}") String url) {
        this.rest = builder.clone().baseUrl(url).build();
    }

    @Override
    public void planejarOrdem(UUID pedidoId, String codigoPedido, LocalDate prazoEntrega, boolean insumosDisponiveis) {
        try {
            rest.post().uri("/producao/ordens")
                    .body(new PlanejamentoRequest(pedidoId, codigoPedido, prazoEntrega, insumosDisponiveis))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw ErrosIntegracao.traduzir(SERVICO, e);
        }
    }

    @Override
    public void cancelarOrdem(UUID pedidoId) {
        try {
            rest.delete().uri("/producao/ordens/pedido/{id}", pedidoId).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw ErrosIntegracao.traduzir(SERVICO, e);
        }
    }
}
