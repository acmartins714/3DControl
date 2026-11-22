package br.edu.uniesp.threedcontrol.estoque.estoque;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Formatos de entrada e saída da API de estoque. */
public final class EstoqueDtos {

    private EstoqueDtos() {
    }

    public record ItemEstoqueRequest(@NotNull UUID insumoId, String nomeInsumo, String unidadeMedida,
                                     @NotNull @DecimalMin("0.0") BigDecimal estoqueMinimo) {
    }

    public record ParametrosRequest(String nomeInsumo, String unidadeMedida,
                                    @NotNull @DecimalMin("0.0") BigDecimal estoqueMinimo) {
    }

    public record EntradaRequest(@NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantidade,
                                 String observacao) {
    }

    /** Quantidade total de um insumo exigida por um pedido. */
    public record NecessidadeRequest(@NotNull UUID insumoId,
                                     @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantidade) {
    }

    public record ReservaRequest(@NotNull UUID pedidoId, @NotEmpty List<@Valid NecessidadeRequest> itens) {
    }

    public record ItemEstoqueResponse(UUID insumoId, String nomeInsumo, String unidadeMedida,
                                      BigDecimal quantidadeDisponivel, BigDecimal quantidadeReservada,
                                      BigDecimal saldoLivre, BigDecimal estoqueMinimo, boolean abaixoDoMinimo) {

        static ItemEstoqueResponse from(ItemEstoque i) {
            return new ItemEstoqueResponse(i.getInsumoId(), i.getNomeInsumo(), i.getUnidadeMedida(),
                    i.getQuantidadeDisponivel(), i.getQuantidadeReservada(), i.saldoLivre(), i.getEstoqueMinimo(),
                    i.abaixoDoMinimo());
        }
    }

    public record MovimentacaoResponse(UUID id, UUID insumoId, MovimentacaoEstoque.Tipo tipo, BigDecimal quantidade,
                                       UUID pedidoId, String observacao, LocalDateTime dataHora) {

        static MovimentacaoResponse from(MovimentacaoEstoque m) {
            return new MovimentacaoResponse(m.getId(), m.getInsumoId(), m.getTipo(), m.getQuantidade(),
                    m.getPedidoId(), m.getObservacao(), m.getDataHora());
        }
    }

    /** Resultado da tentativa de reservar os insumos de um pedido. */
    public record ResultadoReserva(UUID pedidoId, boolean reservado, List<InsumoFaltante> faltantes) {
    }

    public record InsumoFaltante(UUID insumoId, String nomeInsumo, BigDecimal necessario,
                                 BigDecimal disponivel, BigDecimal faltante) {
    }
}
