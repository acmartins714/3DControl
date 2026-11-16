package br.edu.uniesp.threedcontrol.pedidos.pedido.infrastructure.client;

import br.edu.uniesp.threedcontrol.pedidos.shared.application.IntegracaoException;
import br.edu.uniesp.threedcontrol.pedidos.shared.domain.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Converte erros HTTP de outros serviços em exceções do nosso modelo. */
final class ErrosIntegracao {

    private ErrosIntegracao() {
    }

    /** Erros de regra de negócio (422) do serviço remoto viram DomainException com a mesma mensagem. */
    static RuntimeException traduzir(String servico, RestClientException e) {
        if (e instanceof RestClientResponseException r && r.getStatusCode().isSameCodeAs(HttpStatus.UNPROCESSABLE_ENTITY)) {
            ProblemDetail pd = r.getResponseBodyAs(ProblemDetail.class);
            return new DomainException(pd != null && pd.getDetail() != null ? pd.getDetail() : r.getMessage());
        }
        return new IntegracaoException(servico, e);
    }
}
