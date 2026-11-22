package br.edu.uniesp.threedcontrol.pedidos.pedido.integracao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Cliente HTTP do financeiro-service: conta a receber do pedido. */
@Component
public class FinanceiroClient {

    private static final String SERVICO = "financeiro-service";

    public record ItemFinanceiro(String categoria, BigDecimal valorVenda, BigDecimal custoEstimado) {
    }

    public record SituacaoPagamento(boolean quitado, BigDecimal saldoRemanescente) {
    }

    record AberturaRequest(UUID pedidoId, UUID clienteId, List<ItemFinanceiro> itens) {
    }

    record ItensRequest(List<ItemFinanceiro> itens) {
    }

    private final RestClient rest;

    public FinanceiroClient(RestClient.Builder builder, @Value("${integracao.financeiro-url}") String url) {
        this.rest = builder.clone().baseUrl(url).build();
    }

    public void abrirConta(UUID pedidoId, UUID clienteId, List<ItemFinanceiro> itens) {
        try {
            rest.post().uri("/financeiro/contas").body(new AberturaRequest(pedidoId, clienteId, itens))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw ErrosIntegracao.traduzir(SERVICO, e);
        }
    }

    public void atualizarConta(UUID pedidoId, List<ItemFinanceiro> itens) {
        try {
            rest.put().uri("/financeiro/contas/pedido/{id}/itens", pedidoId).body(new ItensRequest(itens))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw ErrosIntegracao.traduzir(SERVICO, e);
        }
    }

    public SituacaoPagamento consultarSituacao(UUID pedidoId) {
        try {
            SituacaoPagamento s = rest.get().uri("/financeiro/contas/pedido/{id}/situacao", pedidoId)
                    .retrieve().body(SituacaoPagamento.class);
            return s == null ? new SituacaoPagamento(false, null) : s;
        } catch (RestClientException e) {
            throw ErrosIntegracao.traduzir(SERVICO, e);
        }
    }

    public void cancelarConta(UUID pedidoId) {
        try {
            rest.delete().uri("/financeiro/contas/pedido/{id}", pedidoId).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw ErrosIntegracao.traduzir(SERVICO, e);
        }
    }
}
