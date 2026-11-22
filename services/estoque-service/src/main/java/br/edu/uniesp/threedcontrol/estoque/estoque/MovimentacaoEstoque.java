package br.edu.uniesp.threedcontrol.estoque.estoque;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Registro imutável (trilha de auditoria) de cada alteração de saldo. */
@Entity
@Table(name = "movimentacao_estoque")
public class MovimentacaoEstoque {

    public enum Tipo { ENTRADA, RESERVA, LIBERACAO_RESERVA, CONSUMO }

    @Id
    private UUID id;
    @Column(name = "insumo_id")
    private UUID insumoId;
    @Enumerated(EnumType.STRING)
    private Tipo tipo;
    private BigDecimal quantidade;
    @Column(name = "pedido_id")
    private UUID pedidoId;
    private String observacao;
    @Column(name = "data_hora")
    private LocalDateTime dataHora;

    protected MovimentacaoEstoque() {
    }

    public MovimentacaoEstoque(UUID insumoId, Tipo tipo, BigDecimal quantidade, UUID pedidoId, String observacao) {
        this.id = UUID.randomUUID();
        this.insumoId = insumoId;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.pedidoId = pedidoId;
        this.observacao = observacao;
        this.dataHora = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getInsumoId() { return insumoId; }
    public Tipo getTipo() { return tipo; }
    public BigDecimal getQuantidade() { return quantidade; }
    public UUID getPedidoId() { return pedidoId; }
    public String getObservacao() { return observacao; }
    public LocalDateTime getDataHora() { return dataHora; }
}
