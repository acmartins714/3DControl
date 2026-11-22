package br.edu.uniesp.threedcontrol.pedidos.pedido.integracao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.UUID;

/** Cliente HTTP do producao-service: inclui, replaneja e cancela ordens na fila. */
@Component
public class ProducaoClient {

    private static final String SERVICO = "producao-service";

    record PlanejamentoRequest(UUID pedidoId, String codigoPedido, LocalDate prazoEntrega,
                               boolean insumosDisponiveis) {
    }

    private final RestClient rest;

    public ProducaoClient(RestClient.Builder builder, @Value("${integracao.producao-url}") String url) {
        this.rest = builder.clone().baseUrl(url).build();
    }

    public void planejarOrdem(UUID pedidoId, String codigoPedido, LocalDate prazoEntrega, boolean insumosDisponiveis) {
        try {
            rest.post().uri("/producao/ordens")
                    .body(new PlanejamentoRequest(pedidoId, codigoPedido, prazoEntrega, insumosDisponiveis))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw ErrosIntegracao.traduzir(SERVICO, e);
        }
    }

    public void cancelarOrdem(UUID pedidoId) {
        try {
            rest.delete().uri("/producao/ordens/pedido/{id}", pedidoId).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw ErrosIntegracao.traduzir(SERVICO, e);
        }
    }
}
