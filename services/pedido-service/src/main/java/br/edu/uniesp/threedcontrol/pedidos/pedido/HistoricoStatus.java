package br.edu.uniesp.threedcontrol.pedidos.pedido;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDateTime;

/** Entrada da linha do tempo do pedido (base do rastreio). */
@Embeddable
public class HistoricoStatus {

    @Enumerated(EnumType.STRING)
    private StatusPedido status;
    @Column(name = "data_hora")
    private LocalDateTime dataHora;
    private String observacao;

    protected HistoricoStatus() {
    }

    HistoricoStatus(StatusPedido status, LocalDateTime dataHora, String observacao) {
        this.status = status;
        this.dataHora = dataHora;
        this.observacao = observacao;
    }

    public StatusPedido getStatus() { return status; }
    public LocalDateTime getDataHora() { return dataHora; }
    public String getObservacao() { return observacao; }
}
