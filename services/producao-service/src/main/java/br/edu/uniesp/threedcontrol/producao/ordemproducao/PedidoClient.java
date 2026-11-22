package br.edu.uniesp.threedcontrol.producao.ordemproducao;

import br.edu.uniesp.threedcontrol.producao.shared.IntegracaoException;
import br.edu.uniesp.threedcontrol.producao.shared.RegraNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

/** Cliente HTTP que avisa o pedido-service sobre o andamento da produção. */
@Component
public class PedidoClient {

    private static final String SERVICO = "pedido-service";

    private final RestClient rest;

    public PedidoClient(RestClient.Builder builder, @Value("${integracao.pedido-url}") String url) {
        this.rest = builder.clone().baseUrl(url).build();
    }

    public void notificarInicioProducao(UUID pedidoId) {
        post("/pedidos/{id}/inicio-producao", pedidoId);
    }

    public void notificarConclusaoProducao(UUID pedidoId) {
        post("/pedidos/{id}/conclusao-producao", pedidoId);
    }

    private void post(String uri, UUID pedidoId) {
        try {
            rest.post().uri(uri, pedidoId).retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            // 422 do pedido-service é uma regra de negócio: repassa a mensagem original
            if (e.getStatusCode().isSameCodeAs(HttpStatus.UNPROCESSABLE_ENTITY)) {
                ProblemDetail pd = e.getResponseBodyAs(ProblemDetail.class);
                throw new RegraNegocioException(pd != null && pd.getDetail() != null ? pd.getDetail() : e.getMessage());
            }
            throw new IntegracaoException(SERVICO, e);
        } catch (RestClientException e) {
            throw new IntegracaoException(SERVICO, e);
        }
    }
}
