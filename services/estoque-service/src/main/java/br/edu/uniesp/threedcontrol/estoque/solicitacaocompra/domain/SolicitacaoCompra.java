package br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.domain;

import br.edu.uniesp.threedcontrol.estoque.shared.domain.DomainException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Demanda de aquisição de insumo sinalizada à administração antes do início da fabricação. */
public class SolicitacaoCompra {

    public enum Status { PENDENTE, RECEBIDA, CANCELADA }

    private final UUID id;
    private final UUID insumoId;
    private final String nomeInsumo;
    private final BigDecimal quantidade;
    private final UUID pedidoId;
    private Status status;
    private final LocalDateTime solicitadaEm;
    private LocalDateTime atualizadaEm;

    private SolicitacaoCompra(UUID id, UUID insumoId, String nomeInsumo, BigDecimal quantidade, UUID pedidoId,
                              Status status, LocalDateTime solicitadaEm, LocalDateTime atualizadaEm) {
        this.id = id;
        this.insumoId = insumoId;
        this.nomeInsumo = nomeInsumo;
        this.quantidade = quantidade;
        this.pedidoId = pedidoId;
        this.status = status;
        this.solicitadaEm = solicitadaEm;
        this.atualizadaEm = atualizadaEm;
    }

    public static SolicitacaoCompra nova(UUID insumoId, String nomeInsumo, BigDecimal quantidade, UUID pedidoId) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new DomainException("A quantidade solicitada deve ser maior que zero.");
        }
        LocalDateTime agora = LocalDateTime.now();
        return new SolicitacaoCompra(UUID.randomUUID(), insumoId, nomeInsumo, quantidade, pedidoId,
                Status.PENDENTE, agora, agora);
    }

    public static SolicitacaoCompra restaurar(UUID id, UUID insumoId, String nomeInsumo, BigDecimal quantidade,
                                              UUID pedidoId, Status status, LocalDateTime solicitadaEm,
                                              LocalDateTime atualizadaEm) {
        return new SolicitacaoCompra(id, insumoId, nomeInsumo, quantidade, pedidoId, status, solicitadaEm, atualizadaEm);
    }

    public void registrarRecebimento() {
        exigirPendente();
        status = Status.RECEBIDA;
        atualizadaEm = LocalDateTime.now();
    }

    public void cancelar() {
        exigirPendente();
        status = Status.CANCELADA;
        atualizadaEm = LocalDateTime.now();
    }

    private void exigirPendente() {
        if (status != Status.PENDENTE) {
            throw new DomainException("A solicitação de compra não está pendente (status atual: " + status + ").");
        }
    }

    public UUID getId() { return id; }
    public UUID getInsumoId() { return insumoId; }
    public String getNomeInsumo() { return nomeInsumo; }
    public BigDecimal getQuantidade() { return quantidade; }
    public UUID getPedidoId() { return pedidoId; }
    public Status getStatus() { return status; }
    public LocalDateTime getSolicitadaEm() { return solicitadaEm; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
}
