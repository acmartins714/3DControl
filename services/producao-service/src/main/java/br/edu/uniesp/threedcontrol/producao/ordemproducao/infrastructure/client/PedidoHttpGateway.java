package br.edu.uniesp.threedcontrol.producao.ordemproducao.infrastructure.client;

import br.edu.uniesp.threedcontrol.producao.ordemproducao.application.port.PedidoGateway;
import br.edu.uniesp.threedcontrol.producao.shared.application.IntegracaoException;
import br.edu.uniesp.threedcontrol.producao.shared.domain.DomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

/** Adaptador HTTP (RestClient) da porta {@link PedidoGateway}. */
@Component
class PedidoHttpGateway implements PedidoGateway {

    private static final String SERVICO = "pedido-service";
    private final RestClient rest;

    PedidoHttpGateway(RestClient.Builder builder, @Value("${integracao.pedido-url}") String url) {
        this.rest = builder.clone().baseUrl(url).build();
    }

    @Override
    public void notificarInicioProducao(UUID pedidoId) {
        post("/pedidos/{id}/inicio-producao", pedidoId);
    }

    @Override
    public void notificarConclusaoProducao(UUID pedidoId) {
        post("/pedidos/{id}/conclusao-producao", pedidoId);
    }

    private void post(String uri, UUID pedidoId) {
        try {
            rest.post().uri(uri, pedidoId).retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.UNPROCESSABLE_ENTITY)) {
                ProblemDetail pd = e.getResponseBodyAs(ProblemDetail.class);
                throw new DomainException(pd != null && pd.getDetail() != null ? pd.getDetail() : e.getMessage());
            }
            throw new IntegracaoException(SERVICO, e);
        } catch (RestClientException e) {
            throw new IntegracaoException(SERVICO, e);
        }
    }
}
