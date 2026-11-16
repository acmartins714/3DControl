package br.edu.uniesp.threedcontrol.pedidos.pedido.infrastructure.client;

import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.FinanceiroGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
class FinanceiroHttpGateway implements FinanceiroGateway {

    private static final String SERVICO = "financeiro-service";

    record AberturaRequest(UUID pedidoId, UUID clienteId, List<ItemFinanceiro> itens) {
    }

    record ItensRequest(List<ItemFinanceiro> itens) {
    }

    record SituacaoDto(UUID pedidoId, BigDecimal saldoRemanescente, boolean quitado) {
    }

    private final RestClient rest;

    FinanceiroHttpGateway(RestClient.Builder builder, @Value("${integracao.financeiro-url}") String url) {
        this.rest = builder.clone().baseUrl(url).build();
    }

    @Override
    public void abrirConta(UUID pedidoId, UUID clienteId, List<ItemFinanceiro> itens) {
        try {
            rest.post().uri("/financeiro/contas").body(new AberturaRequest(pedidoId, clienteId, itens))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw ErrosIntegracao.traduzir(SERVICO, e);
        }
    }

    @Override
    public void atualizarConta(UUID pedidoId, List<ItemFinanceiro> itens) {
        try {
            rest.put().uri("/financeiro/contas/pedido/{id}/itens", pedidoId).body(new ItensRequest(itens))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw ErrosIntegracao.traduzir(SERVICO, e);
        }
    }

    @Override
    public SituacaoPagamento consultarSituacao(UUID pedidoId) {
        try {
            SituacaoDto s = rest.get().uri("/financeiro/contas/pedido/{id}/situacao", pedidoId)
                    .retrieve().body(SituacaoDto.class);
            return s == null ? new SituacaoPagamento(false, null) : new SituacaoPagamento(s.quitado(), s.saldoRemanescente());
        } catch (RestClientException e) {
            throw ErrosIntegracao.traduzir(SERVICO, e);
        }
    }

    @Override
    public void cancelarConta(UUID pedidoId) {
        try {
            rest.delete().uri("/financeiro/contas/pedido/{id}", pedidoId).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw ErrosIntegracao.traduzir(SERVICO, e);
        }
    }
}
