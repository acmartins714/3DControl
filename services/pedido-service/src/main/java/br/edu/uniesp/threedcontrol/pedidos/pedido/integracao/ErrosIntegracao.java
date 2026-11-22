package br.edu.uniesp.threedcontrol.pedidos.pedido.integracao;

import br.edu.uniesp.threedcontrol.pedidos.shared.IntegracaoException;
import br.edu.uniesp.threedcontrol.pedidos.shared.RegraNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Converte erros HTTP dos outros serviços nas exceções deste serviço. */
final class ErrosIntegracao {

    private ErrosIntegracao() {
    }

    /** Erro de regra de negócio (422) do serviço remoto vira {@link RegraNegocioException} com a mesma mensagem. */
    static RuntimeException traduzir(String servico, RestClientException e) {
        if (e instanceof RestClientResponseException r && r.getStatusCode().isSameCodeAs(HttpStatus.UNPROCESSABLE_ENTITY)) {
            ProblemDetail pd = r.getResponseBodyAs(ProblemDetail.class);
            return new RegraNegocioException(pd != null && pd.getDetail() != null ? pd.getDetail() : r.getMessage());
        }
        return new IntegracaoException(servico, e);
    }
}
