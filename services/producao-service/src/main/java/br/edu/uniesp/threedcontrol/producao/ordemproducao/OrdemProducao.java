package br.edu.uniesp.threedcontrol.producao.ordemproducao;

import br.edu.uniesp.threedcontrol.producao.shared.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ordem de produção de um pedido. Guarda apenas os dados de planejamento: as especificações
 * do pedido são sempre consultadas no pedido-service (fonte única), evitando produzir com dados antigos.
 */
@Entity
@Table(name = "ordem_producao")
public class OrdemProducao {

    @Id
    private UUID id;
    @Column(name = "pedido_id")
    private UUID pedidoId;
    @Column(name = "codigo_pedido")
    private String codigoPedido;
    @Column(name = "prazo_entrega")
    private LocalDate prazoEntrega;
    @Column(name = "insumos_disponiveis")
    private boolean insumosDisponiveis;
    @Enumerated(EnumType.STRING)
    private StatusOrdem status;
    private String responsavel;
    private int retrabalhos;
    private String observacoes;
    @Column(name = "criada_em")
    private LocalDateTime criadaEm;
    @Column(name = "iniciada_em")
    private LocalDateTime iniciadaEm;
    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;

    protected OrdemProducao() {
    }

    public OrdemProducao(UUID pedidoId, String codigoPedido, LocalDate prazoEntrega, boolean insumosDisponiveis) {
        if (pedidoId == null || prazoEntrega == null) {
            throw new RegraNegocioException("Pedido e prazo de entrega são obrigatórios para a ordem de produção.");
        }
        this.id = UUID.randomUUID();
        this.pedidoId = pedidoId;
        this.codigoPedido = codigoPedido;
        this.prazoEntrega = prazoEntrega;
        this.insumosDisponiveis = insumosDisponiveis;
        this.status = StatusOrdem.AGUARDANDO;
        this.criadaEm = LocalDateTime.now();
    }

    /** Replaneja a ordem (prazo/insumos). Só é possível enquanto ela ainda aguarda na fila. */
    public void replanejar(LocalDate prazoEntrega, boolean insumosDisponiveis) {
        if (status != StatusOrdem.AGUARDANDO) {
            throw new RegraNegocioException("A ordem do pedido " + codigoPedido + " não pode ser replanejada no status " + status + ".");
        }
        this.prazoEntrega = prazoEntrega;
        this.insumosDisponiveis = insumosDisponiveis;
    }

    public void iniciar(String responsavel) {
        if (status != StatusOrdem.AGUARDANDO) {
            throw new RegraNegocioException("Somente ordens aguardando podem ser iniciadas.");
        }
        if (!insumosDisponiveis) {
            throw new RegraNegocioException("A ordem do pedido " + codigoPedido + " ainda aguarda insumos.");
        }
        this.status = StatusOrdem.EM_PRODUCAO;
        this.responsavel = responsavel;
        this.iniciadaEm = LocalDateTime.now();
    }

    public void registrarRetrabalho(String motivo) {
        if (status != StatusOrdem.EM_PRODUCAO) {
            throw new RegraNegocioException("Retrabalho só pode ser registrado em ordens em produção.");
        }
        retrabalhos++;
        observacoes = (observacoes == null ? "" : observacoes + "\n") + "Retrabalho: " + motivo;
    }

    public void concluir() {
        if (status != StatusOrdem.EM_PRODUCAO) {
            throw new RegraNegocioException("Somente ordens em produção podem ser concluídas.");
        }
        this.status = StatusOrdem.CONCLUIDA;
        this.concluidaEm = LocalDateTime.now();
    }

    public void cancelar() {
        if (status == StatusOrdem.CONCLUIDA) {
            throw new RegraNegocioException("Uma ordem concluída não pode ser cancelada.");
        }
        this.status = StatusOrdem.CANCELADA;
    }

    public Prioridade prioridade(LocalDate hoje) {
        return Prioridade.para(prazoEntrega, hoje);
    }

    public boolean estaAtrasada(LocalDate hoje) {
        return status != StatusOrdem.CONCLUIDA && status != StatusOrdem.CANCELADA && hoje.isAfter(prazoEntrega);
    }

    public UUID getId() { return id; }
    public UUID getPedidoId() { return pedidoId; }
    public String getCodigoPedido() { return codigoPedido; }
    public LocalDate getPrazoEntrega() { return prazoEntrega; }
    public boolean isInsumosDisponiveis() { return insumosDisponiveis; }
    public StatusOrdem getStatus() { return status; }
    public String getResponsavel() { return responsavel; }
    public int getRetrabalhos() { return retrabalhos; }
    public String getObservacoes() { return observacoes; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public LocalDateTime getIniciadaEm() { return iniciadaEm; }
    public LocalDateTime getConcluidaEm() { return concluidaEm; }
}
