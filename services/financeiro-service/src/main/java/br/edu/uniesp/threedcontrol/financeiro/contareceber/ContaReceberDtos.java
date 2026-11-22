package br.edu.uniesp.threedcontrol.financeiro.contareceber;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Formatos de entrada e saída da API de contas a receber. */
public final class ContaReceberDtos {

    private ContaReceberDtos() {
    }

    public record ItemRequest(@NotBlank String categoria,
                              @NotNull @DecimalMin("0.0") BigDecimal valorVenda,
                              @NotNull @DecimalMin("0.0") BigDecimal custoEstimado) {

        ItemConta toItemConta() {
            return new ItemConta(categoria, valorVenda, custoEstimado);
        }

        static List<ItemConta> converter(List<ItemRequest> itens) {
            return itens.stream().map(ItemRequest::toItemConta).toList();
        }
    }

    public record AberturaRequest(@NotNull UUID pedidoId, @NotNull UUID clienteId,
                                  @NotEmpty List<@Valid ItemRequest> itens) {
    }

    public record ItensRequest(@NotEmpty List<@Valid ItemRequest> itens) {
    }

    public record PagamentoRequest(@NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal valor,
                                   @NotNull Pagamento.FormaPagamento forma, String observacao) {
    }

    public record ItemResponse(String categoria, BigDecimal valorVenda, BigDecimal custoEstimado) {
    }

    public record PagamentoResponse(UUID id, BigDecimal valor, Pagamento.TipoPagamento tipo,
                                    Pagamento.FormaPagamento forma, LocalDateTime pagoEm, String observacao) {
    }

    public record ContaResponse(UUID id, UUID pedidoId, UUID clienteId, StatusConta status, BigDecimal valorTotal,
                                BigDecimal custoEstimado, BigDecimal totalPago, BigDecimal saldoRemanescente,
                                boolean liberadaParaEntrega, List<ItemResponse> itens, List<PagamentoResponse> pagamentos,
                                LocalDateTime criadaEm, LocalDateTime atualizadaEm) {

        static ContaResponse from(ContaReceber c) {
            return new ContaResponse(c.getId(), c.getPedidoId(), c.getClienteId(), c.getStatus(), c.valorTotal(),
                    c.custoTotal(), c.totalPago(), c.saldoRemanescente(), c.liberadaParaEntrega(),
                    c.getItens().stream()
                            .map(i -> new ItemResponse(i.getCategoria(), i.getValorVenda(), i.getCustoEstimado()))
                            .toList(),
                    c.getPagamentos().stream()
                            .map(p -> new PagamentoResponse(p.getId(), p.getValor(), p.getTipo(), p.getForma(),
                                    p.getPagoEm(), p.getObservacao()))
                            .toList(),
                    c.getCriadaEm(), c.getAtualizadaEm());
        }
    }

    /** Resposta usada pelo pedido-service para autorizar a entrega. */
    public record SituacaoFinanceira(UUID pedidoId, BigDecimal valorTotal, BigDecimal totalPago,
                                     BigDecimal saldoRemanescente, StatusConta status, boolean quitado) {

        static SituacaoFinanceira from(ContaReceber c) {
            return new SituacaoFinanceira(c.getPedidoId(), c.valorTotal(), c.totalPago(), c.saldoRemanescente(),
                    c.getStatus(), c.liberadaParaEntrega());
        }
    }
}
