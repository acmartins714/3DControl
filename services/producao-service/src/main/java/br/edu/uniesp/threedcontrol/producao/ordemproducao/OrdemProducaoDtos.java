package br.edu.uniesp.threedcontrol.producao.ordemproducao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Formatos de entrada e saída da API de produção. */
public final class OrdemProducaoDtos {

    private OrdemProducaoDtos() {
    }

    public record PlanejamentoRequest(@NotNull UUID pedidoId, String codigoPedido, @NotNull LocalDate prazoEntrega,
                                      boolean insumosDisponiveis) {
    }

    public record InicioRequest(@NotBlank String responsavel) {
    }

    public record RetrabalhoRequest(@NotBlank String motivo) {
    }

    public record OrdemResponse(UUID id, UUID pedidoId, String codigoPedido, LocalDate prazoEntrega,
                                boolean insumosDisponiveis, StatusOrdem status, Prioridade prioridade, boolean atrasada,
                                String responsavel, int retrabalhos, String observacoes, LocalDateTime criadaEm,
                                LocalDateTime iniciadaEm, LocalDateTime concluidaEm) {

        static OrdemResponse from(OrdemProducao o, LocalDate hoje) {
            return new OrdemResponse(o.getId(), o.getPedidoId(), o.getCodigoPedido(), o.getPrazoEntrega(),
                    o.isInsumosDisponiveis(), o.getStatus(), o.prioridade(hoje), o.estaAtrasada(hoje),
                    o.getResponsavel(), o.getRetrabalhos(), o.getObservacoes(), o.getCriadaEm(), o.getIniciadaEm(),
                    o.getConcluidaEm());
        }
    }
}
