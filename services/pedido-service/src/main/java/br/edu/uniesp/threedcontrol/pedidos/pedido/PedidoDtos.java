package br.edu.uniesp.threedcontrol.pedidos.pedido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Formatos de entrada e saída da API de pedidos. */
public final class PedidoDtos {

    private PedidoDtos() {
    }

    /** {@code precoUnitario} é opcional: se ausente, usa o preço base do catálogo. */
    public record ItemRequest(@NotNull UUID produtoId, @Positive int quantidade,
                              @DecimalMin("0.0") BigDecimal precoUnitario, String personalizacao) {
    }

    public record NovoPedidoRequest(@NotNull UUID clienteId, @NotNull CanalAtendimento canalOrigem,
                                    String atendenteResponsavel, @NotNull @FutureOrPresent LocalDate prazoEntrega,
                                    @NotNull ModalidadeEntrega modalidadeEntrega,
                                    @NotNull CondicaoPagamento condicaoPagamento,
                                    String observacoes, @NotEmpty List<@Valid ItemRequest> itens) {
    }

    public record AlteracaoRequest(@NotNull @FutureOrPresent LocalDate prazoEntrega,
                                   @NotNull ModalidadeEntrega modalidadeEntrega,
                                   @NotNull CondicaoPagamento condicaoPagamento, String observacoes,
                                   @NotEmpty List<@Valid ItemRequest> itens) {
    }

    public record CancelamentoRequest(String motivo) {
    }

    public record ItemResponse(UUID produtoId, String nomeProduto, String categoria, int quantidade,
                               BigDecimal precoUnitario, BigDecimal custoUnitarioEstimado, String personalizacao) {
    }

    public record HistoricoResponse(StatusPedido status, LocalDateTime dataHora, String observacao) {
    }

    public record PedidoResponse(UUID id, String codigo, UUID clienteId, CanalAtendimento canalOrigem,
                                 String atendenteResponsavel, LocalDate prazoEntrega,
                                 ModalidadeEntrega modalidadeEntrega, CondicaoPagamento condicaoPagamento,
                                 String observacoes, StatusPedido status, boolean permiteAlteracao,
                                 BigDecimal valorTotal, BigDecimal custoEstimado, List<ItemResponse> itens,
                                 List<HistoricoResponse> historico, LocalDateTime criadoEm,
                                 LocalDateTime atualizadoEm) {

        static PedidoResponse from(Pedido p) {
            return new PedidoResponse(p.getId(), p.getCodigo(), p.getClienteId(), p.getCanalOrigem(),
                    p.getAtendenteResponsavel(), p.getPrazoEntrega(), p.getModalidadeEntrega(),
                    p.getCondicaoPagamento(), p.getObservacoes(), p.getStatus(), p.getStatus().permiteAlteracao(),
                    p.valorTotal(), p.custoEstimadoTotal(),
                    p.getItens().stream().map(i -> new ItemResponse(i.getProdutoId(), i.getNomeProduto(),
                            i.getCategoria(), i.getQuantidade(), i.getPrecoUnitario(), i.getCustoUnitarioEstimado(),
                            i.getPersonalizacao())).toList(),
                    p.getHistorico().stream().map(h -> new HistoricoResponse(h.getStatus(), h.getDataHora(),
                            h.getObservacao())).toList(),
                    p.getCriadoEm(), p.getAtualizadoEm());
        }
    }
}
