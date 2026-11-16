package br.edu.uniesp.threedcontrol.producao.ordemproducao.domain;

import br.edu.uniesp.threedcontrol.producao.shared.domain.DomainException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ordem de produção de um pedido. Guarda apenas os dados de planejamento: as especificações
 * do pedido são sempre consultadas no pedido-service (fonte única), evitando produzir com dados antigos.
 */
public class OrdemProducao {

    private final UUID id;
    private final UUID pedidoId;
    private final String codigoPedido;
    private LocalDate prazoEntrega;
    private boolean insumosDisponiveis;
    private StatusOrdem status;
    private String responsavel;
    private int retrabalhos;
    private String observacoes;
    private final LocalDateTime criadaEm;
    private LocalDateTime iniciadaEm;
    private LocalDateTime concluidaEm;

    private OrdemProducao(UUID id, UUID pedidoId, String codigoPedido, LocalDate prazoEntrega,
                          boolean insumosDisponiveis, StatusOrdem status, String responsavel, int retrabalhos,
                          String observacoes, LocalDateTime criadaEm, LocalDateTime iniciadaEm,
                          LocalDateTime concluidaEm) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.codigoPedido = codigoPedido;
        this.prazoEntrega = prazoEntrega;
        this.insumosDisponiveis = insumosDisponiveis;
        this.status = status;
        this.responsavel = responsavel;
        this.retrabalhos = retrabalhos;
        this.observacoes = observacoes;
        this.criadaEm = criadaEm;
        this.iniciadaEm = iniciadaEm;
        this.concluidaEm = concluidaEm;
    }

    public static OrdemProducao nova(UUID pedidoId, String codigoPedido, LocalDate prazoEntrega, boolean insumosDisponiveis) {
        if (pedidoId == null || prazoEntrega == null) {
            throw new DomainException("Pedido e prazo de entrega são obrigatórios para a ordem de produção.");
        }
        return new OrdemProducao(UUID.randomUUID(), pedidoId, codigoPedido, prazoEntrega, insumosDisponiveis,
                StatusOrdem.AGUARDANDO, null, 0, null, LocalDateTime.now(), null, null);
    }

    public static OrdemProducao restaurar(UUID id, UUID pedidoId, String codigoPedido, LocalDate prazoEntrega,
                                          boolean insumosDisponiveis, StatusOrdem status, String responsavel,
                                          int retrabalhos, String observacoes, LocalDateTime criadaEm,
                                          LocalDateTime iniciadaEm, LocalDateTime concluidaEm) {
        return new OrdemProducao(id, pedidoId, codigoPedido, prazoEntrega, insumosDisponiveis, status, responsavel,
                retrabalhos, observacoes, criadaEm, iniciadaEm, concluidaEm);
    }

    /** Replaneja a ordem (prazo/insumos). Só é possível enquanto ela ainda aguarda na fila. */
    public void replanejar(LocalDate prazoEntrega, boolean insumosDisponiveis) {
        if (status != StatusOrdem.AGUARDANDO) {
            throw new DomainException("A ordem do pedido " + codigoPedido + " não pode ser replanejada no status " + status + ".");
        }
        this.prazoEntrega = prazoEntrega;
        this.insumosDisponiveis = insumosDisponiveis;
    }

    public void iniciar(String responsavel) {
        if (status != StatusOrdem.AGUARDANDO) {
            throw new DomainException("Somente ordens aguardando podem ser iniciadas.");
        }
        if (!insumosDisponiveis) {
            throw new DomainException("A ordem do pedido " + codigoPedido + " ainda aguarda insumos.");
        }
        this.status = StatusOrdem.EM_PRODUCAO;
        this.responsavel = responsavel;
        this.iniciadaEm = LocalDateTime.now();
    }

    public void registrarRetrabalho(String motivo) {
        if (status != StatusOrdem.EM_PRODUCAO) {
            throw new DomainException("Retrabalho só pode ser registrado em ordens em produção.");
        }
        retrabalhos++;
        observacoes = (observacoes == null ? "" : observacoes + "\n") + "Retrabalho: " + motivo;
    }

    public void concluir() {
        if (status != StatusOrdem.EM_PRODUCAO) {
            throw new DomainException("Somente ordens em produção podem ser concluídas.");
        }
        this.status = StatusOrdem.CONCLUIDA;
        this.concluidaEm = LocalDateTime.now();
    }

    public void cancelar() {
        if (status == StatusOrdem.CONCLUIDA) {
            throw new DomainException("Uma ordem concluída não pode ser cancelada.");
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
