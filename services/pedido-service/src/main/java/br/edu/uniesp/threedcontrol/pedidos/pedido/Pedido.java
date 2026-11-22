package br.edu.uniesp.threedcontrol.pedidos.pedido;

import br.edu.uniesp.threedcontrol.pedidos.shared.RegraNegocioException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pedido de um cliente. Concentra as regras centrais do negócio:
 * <ul>
 *   <li>Regra de Ouro: alterações só antes da produção;</li>
 *   <li>Máquina de estados do ciclo de vida, com histórico (rastreio);</li>
 *   <li>Entrega condicionada à quitação do saldo.</li>
 * </ul>
 */
@Entity
@Table(name = "pedido")
public class Pedido {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    @Id
    private UUID id;
    private String codigo;
    @Column(name = "cliente_id")
    private UUID clienteId;
    @Enumerated(EnumType.STRING)
    @Column(name = "canal_origem")
    private CanalAtendimento canalOrigem;
    @Column(name = "atendente_responsavel")
    private String atendenteResponsavel;
    @Column(name = "prazo_entrega")
    private LocalDate prazoEntrega;
    @Enumerated(EnumType.STRING)
    @Column(name = "modalidade_entrega")
    private ModalidadeEntrega modalidadeEntrega;
    @Enumerated(EnumType.STRING)
    @Column(name = "condicao_pagamento")
    private CondicaoPagamento condicaoPagamento;
    private String observacoes;
    @Enumerated(EnumType.STRING)
    private StatusPedido status;
    @Column(name = "criado_em")
    private LocalDateTime criadoEm;
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    /** Controle de concorrência otimista: duas alterações simultâneas no mesmo pedido geram HTTP 409. */
    @Version
    private Long versao;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pedido_item", joinColumns = @JoinColumn(name = "pedido_id"))
    private List<ItemPedido> itens = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pedido_historico", joinColumns = @JoinColumn(name = "pedido_id"))
    @OrderBy("dataHora")
    private List<HistoricoStatus> historico = new ArrayList<>();

    protected Pedido() {
    }

    public Pedido(UUID clienteId, CanalAtendimento canalOrigem, String atendenteResponsavel,
                  LocalDate prazoEntrega, ModalidadeEntrega modalidadeEntrega,
                  CondicaoPagamento condicaoPagamento, String observacoes, List<ItemPedido> itens) {
        if (clienteId == null) {
            throw new RegraNegocioException("O cliente é obrigatório.");
        }
        if (canalOrigem == null) {
            throw new RegraNegocioException("O canal de origem do pedido é obrigatório.");
        }
        boolean semAtendente = atendenteResponsavel == null || atendenteResponsavel.isBlank();
        if (canalOrigem != CanalAtendimento.SITE && semAtendente) {
            throw new RegraNegocioException("Informe o funcionário que realizou o atendimento.");
        }
        LocalDateTime agora = LocalDateTime.now();
        this.id = UUID.randomUUID();
        this.codigo = gerarCodigo(agora);
        this.clienteId = clienteId;
        this.canalOrigem = canalOrigem;
        this.atendenteResponsavel = semAtendente ? "Site (pedido automatizado)" : atendenteResponsavel;
        this.status = StatusPedido.EM_TRIAGEM;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
        definirDetalhes(prazoEntrega, modalidadeEntrega, condicaoPagamento, observacoes, itens);
        this.historico.add(new HistoricoStatus(StatusPedido.EM_TRIAGEM, agora, "Pedido registrado via " + canalOrigem));
    }

    // ------------------------------------------------------------------ Regra de Ouro

    public void garantirAlteravel() {
        if (!status.permiteAlteracao()) {
            throw new PedidoNaoAlteravelException(codigo, status);
        }
    }

    public void alterar(LocalDate prazoEntrega, ModalidadeEntrega modalidadeEntrega,
                        CondicaoPagamento condicaoPagamento, String observacoes, List<ItemPedido> itens) {
        garantirAlteravel();
        definirDetalhes(prazoEntrega, modalidadeEntrega, condicaoPagamento, observacoes, itens);
        registrarHistorico(status, "Pedido alterado pelo atendimento");
    }

    private void definirDetalhes(LocalDate prazoEntrega, ModalidadeEntrega modalidadeEntrega,
                                 CondicaoPagamento condicaoPagamento, String observacoes, List<ItemPedido> itens) {
        if (prazoEntrega == null || prazoEntrega.isBefore(LocalDate.now())) {
            throw new RegraNegocioException("O prazo de entrega deve ser hoje ou uma data futura.");
        }
        if (modalidadeEntrega == null || condicaoPagamento == null) {
            throw new RegraNegocioException("Modalidade de entrega e condição de pagamento são obrigatórias.");
        }
        if (itens == null || itens.isEmpty()) {
            throw new RegraNegocioException("O pedido deve ter ao menos um item.");
        }
        this.prazoEntrega = prazoEntrega;
        this.modalidadeEntrega = modalidadeEntrega;
        this.condicaoPagamento = condicaoPagamento;
        this.observacoes = observacoes;
        this.itens.clear();
        this.itens.addAll(itens);
    }

    // ------------------------------------------------------------------ Ciclo de vida

    /** Ajusta o status conforme o resultado da reserva de insumos no estoque. */
    public void atualizarDisponibilidadeInsumos(boolean disponiveis, String detalhe) {
        if (disponiveis && status == StatusPedido.AGUARDANDO_INSUMO) {
            transitarPara(StatusPedido.EM_TRIAGEM, "Insumos reservados");
        } else if (!disponiveis && status == StatusPedido.EM_TRIAGEM) {
            transitarPara(StatusPedido.AGUARDANDO_INSUMO, detalhe);
        }
    }

    public void iniciarProducao() {
        if (status == StatusPedido.AGUARDANDO_INSUMO) {
            throw new RegraNegocioException("O pedido " + codigo + " ainda aguarda insumos e não pode entrar em produção.");
        }
        transitarPara(StatusPedido.EM_PRODUCAO, "Produção iniciada — pedido bloqueado para alterações");
    }

    public void finalizarProducao() {
        transitarPara(StatusPedido.FINALIZADO, "Produção concluída");
    }

    public void despachar() {
        if (modalidadeEntrega != ModalidadeEntrega.ENTREGA) {
            throw new RegraNegocioException("O pedido " + codigo + " é para retirada e não pode ser despachado.");
        }
        transitarPara(StatusPedido.EM_ROTA_ENTREGA, "Saiu para entrega");
    }

    /** Validação na entrega: o produto só é liberado ao cliente com o saldo integralmente quitado. */
    public void confirmarEntrega(boolean pagamentoQuitado, BigDecimal saldoRemanescente) {
        if (!pagamentoQuitado) {
            throw new RegraNegocioException("Entrega bloqueada: o pedido " + codigo + " possui saldo remanescente de R$ "
                    + saldoRemanescente + ". Registre a quitação no financeiro antes de liberar o produto.");
        }
        transitarPara(StatusPedido.ENTREGUE, "Entregue ao cliente com pagamento quitado");
    }

    public void cancelar(String motivo) {
        transitarPara(StatusPedido.CANCELADO, motivo == null || motivo.isBlank() ? "Pedido cancelado" : motivo);
    }

    private void transitarPara(StatusPedido destino, String observacao) {
        if (!status.podeTransitarPara(destino)) {
            throw new RegraNegocioException("Transição de status inválida para o pedido " + codigo + ": "
                    + status.descricao() + " → " + destino.descricao() + ".");
        }
        status = destino;
        registrarHistorico(destino, observacao);
    }

    private void registrarHistorico(StatusPedido s, String observacao) {
        atualizadoEm = LocalDateTime.now();
        historico.add(new HistoricoStatus(s, atualizadoEm, observacao));
    }

    // ------------------------------------------------------------------ Cálculos

    public BigDecimal valorTotal() {
        return itens.stream().map(ItemPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal custoEstimadoTotal() {
        return itens.stream().map(ItemPedido::custoTotalEstimado).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String gerarCodigo(LocalDateTime agora) {
        StringBuilder sufixo = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sufixo.append(ALFABETO.charAt(ThreadLocalRandom.current().nextInt(ALFABETO.length())));
        }
        return "3DC-" + agora.format(DateTimeFormatter.ofPattern("yyMMdd")) + "-" + sufixo;
    }

    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public UUID getClienteId() { return clienteId; }
    public CanalAtendimento getCanalOrigem() { return canalOrigem; }
    public String getAtendenteResponsavel() { return atendenteResponsavel; }
    public LocalDate getPrazoEntrega() { return prazoEntrega; }
    public ModalidadeEntrega getModalidadeEntrega() { return modalidadeEntrega; }
    public CondicaoPagamento getCondicaoPagamento() { return condicaoPagamento; }
    public String getObservacoes() { return observacoes; }
    public List<ItemPedido> getItens() { return Collections.unmodifiableList(itens); }
    public StatusPedido getStatus() { return status; }
    public List<HistoricoStatus> getHistorico() { return Collections.unmodifiableList(historico); }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
