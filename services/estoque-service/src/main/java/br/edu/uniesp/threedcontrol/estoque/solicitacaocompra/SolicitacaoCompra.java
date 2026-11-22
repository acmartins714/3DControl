package br.edu.uniesp.threedcontrol.estoque.solicitacaocompra;

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

/** Demanda de aquisição de insumo sinalizada à administração antes do início da fabricação. */
@Entity
@Table(name = "solicitacao_compra")
public class SolicitacaoCompra {

    public enum Status { PENDENTE, RECEBIDA, CANCELADA }

    @Id
    private UUID id;
    @Column(name = "insumo_id")
    private UUID insumoId;
    @Column(name = "nome_insumo")
    private String nomeInsumo;
    private BigDecimal quantidade;
    @Column(name = "pedido_id")
    private UUID pedidoId;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column(name = "solicitada_em")
    private LocalDateTime solicitadaEm;
    @Column(name = "atualizada_em")
    private LocalDateTime atualizadaEm;

    protected SolicitacaoCompra() {
    }

    public SolicitacaoCompra(UUID insumoId, String nomeInsumo, BigDecimal quantidade, UUID pedidoId) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new RegraNegocioException("A quantidade solicitada deve ser maior que zero.");
        }
        this.id = UUID.randomUUID();
        this.insumoId = insumoId;
        this.nomeInsumo = nomeInsumo;
        this.quantidade = quantidade;
        this.pedidoId = pedidoId;
        this.status = Status.PENDENTE;
        this.solicitadaEm = LocalDateTime.now();
        this.atualizadaEm = this.solicitadaEm;
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
            throw new RegraNegocioException("A solicitação de compra não está pendente (status atual: " + status + ").");
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
