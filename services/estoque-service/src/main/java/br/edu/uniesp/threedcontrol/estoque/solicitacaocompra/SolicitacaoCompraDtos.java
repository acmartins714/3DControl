package br.edu.uniesp.threedcontrol.estoque.solicitacaocompra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Formatos de entrada e saída da API de solicitações de compra. */
public final class SolicitacaoCompraDtos {

    private SolicitacaoCompraDtos() {
    }

    public record RecebimentoRequest(BigDecimal quantidadeRecebida) {
    }

    public record SolicitacaoResponse(UUID id, UUID insumoId, String nomeInsumo, BigDecimal quantidade, UUID pedidoId,
                                      SolicitacaoCompra.Status status, LocalDateTime solicitadaEm,
                                      LocalDateTime atualizadaEm) {

        static SolicitacaoResponse from(SolicitacaoCompra s) {
            return new SolicitacaoResponse(s.getId(), s.getInsumoId(), s.getNomeInsumo(), s.getQuantidade(),
                    s.getPedidoId(), s.getStatus(), s.getSolicitadaEm(), s.getAtualizadaEm());
        }
    }
}
