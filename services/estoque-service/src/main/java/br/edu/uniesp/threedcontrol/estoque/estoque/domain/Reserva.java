package br.edu.uniesp.threedcontrol.estoque.estoque.domain;

import br.edu.uniesp.threedcontrol.estoque.shared.domain.DomainException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Quantidade de um insumo comprometida com um pedido específico. */
public class Reserva {

    public enum Status { ATIVA, CONSUMIDA, CANCELADA }

    private final UUID id;
    private final UUID pedidoId;
    private final UUID insumoId;
    private final BigDecimal quantidade;
    private Status status;
    private final LocalDateTime criadaEm;

    private Reserva(UUID id, UUID pedidoId, UUID insumoId, BigDecimal quantidade, Status status, LocalDateTime criadaEm) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.insumoId = insumoId;
        this.quantidade = quantidade;
        this.status = status;
        this.criadaEm = criadaEm;
    }

    public static Reserva nova(UUID pedidoId, UUID insumoId, BigDecimal quantidade) {
        return new Reserva(UUID.randomUUID(), pedidoId, insumoId, quantidade, Status.ATIVA, LocalDateTime.now());
    }

    public static Reserva restaurar(UUID id, UUID pedidoId, UUID insumoId, BigDecimal quantidade,
                                    Status status, LocalDateTime criadaEm) {
        return new Reserva(id, pedidoId, insumoId, quantidade, status, criadaEm);
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
            throw new DomainException("A reserva não está ativa.");
        }
    }

    public UUID getId() { return id; }
    public UUID getPedidoId() { return pedidoId; }
    public UUID getInsumoId() { return insumoId; }
    public BigDecimal getQuantidade() { return quantidade; }
    public Status getStatus() { return status; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
}
