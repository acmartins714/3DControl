package br.edu.uniesp.threedcontrol.estoque.estoque.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Registro imutável (trilha de auditoria) de cada alteração de saldo. */
public record MovimentacaoEstoque(UUID id, UUID insumoId, Tipo tipo, BigDecimal quantidade,
                                  UUID pedidoId, String observacao, LocalDateTime dataHora) {

    public enum Tipo { ENTRADA, RESERVA, LIBERACAO_RESERVA, CONSUMO }

    public static MovimentacaoEstoque de(UUID insumoId, Tipo tipo, BigDecimal quantidade, UUID pedidoId, String observacao) {
        return new MovimentacaoEstoque(UUID.randomUUID(), insumoId, tipo, quantidade, pedidoId, observacao,
                LocalDateTime.now());
    }
}
