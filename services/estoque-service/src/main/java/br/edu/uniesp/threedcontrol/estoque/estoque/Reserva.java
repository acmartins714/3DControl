package br.edu.uniesp.threedcontrol.estoque.estoque;

import br.edu.uniesp.threedcontrol.estoque.shared.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Quantidade de um insumo comprometida com um pedido específico. */
@Entity
@Table(name = "reserva")
public class Reserva {

    public enum Status { ATIVA, CONSUMIDA, CANCELADA }

    @Id
    private UUID id;
    @Column(name = "pedido_id")
    private UUID pedidoId;
    @Column(name = "insumo_id")
    private UUID insumoId;
    private BigDecimal quantidade;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column(name = "criada_em")
    private LocalDateTime criadaEm;

    protected Reserva() {
    }

    public Reserva(UUID pedidoId, UUID insumoId, BigDecimal quantidade) {
        this.id = UUID.randomUUID();
        this.pedidoId = pedidoId;
        this.insumoId = insumoId;
        this.quantidade = quantidade;
        this.status = Status.ATIVA;
        this.criadaEm = LocalDateTime.now();
    }

    public void consumir() {
        exigirAtiva();
        status = Status.CONSUMIDA;
    }

    public void cancelar() {
        exigirAtiva();
        status = Status.CANCELADA;
    }

    private void exigirAtiva() {
        if (status != Status.ATIVA) {
            throw new RegraNegocioException("A reserva não está ativa.");
        }
    }

    public UUID getId() { return id; }
    public UUID getPedidoId() { return pedidoId; }
    public UUID getInsumoId() { return insumoId; }
    public BigDecimal getQuantidade() { return quantidade; }
    public Status getStatus() { return status; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
}
