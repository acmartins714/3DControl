package br.edu.uniesp.threedcontrol.pedidos.pedido.domain;

import java.time.LocalDateTime;

/** Entrada da linha do tempo do pedido (base do rastreio). */
public record HistoricoStatus(StatusPedido status, LocalDateTime dataHora, String observacao) {
}
