package br.edu.uniesp.threedcontrol.pedidos.pedido.application.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Porta de saída para o financeiro-service (contas a receber). */
public interface FinanceiroGateway {

    record ItemFinanceiro(String categoria, BigDecimal valorVenda, BigDecimal custoEstimado) {
    }

    record SituacaoPagamento(boolean quitado, BigDecimal saldoRemanescente) {
    }

    void abrirConta(UUID pedidoId, UUID clienteId, List<ItemFinanceiro> itens);

    void atualizarConta(UUID pedidoId, List<ItemFinanceiro> itens);

    SituacaoPagamento consultarSituacao(UUID pedidoId);

    void cancelarConta(UUID pedidoId);
}
