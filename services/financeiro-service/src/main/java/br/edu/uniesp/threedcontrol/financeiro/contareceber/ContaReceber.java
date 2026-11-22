package br.edu.uniesp.threedcontrol.financeiro.contareceber;

import br.edu.uniesp.threedcontrol.financeiro.contareceber.Pagamento.FormaPagamento;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.Pagamento.TipoPagamento;
import br.edu.uniesp.threedcontrol.financeiro.shared.RegraNegocioException;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Controla quanto o cliente deve por um pedido e o que já foi pago.
 * É a fonte da verdade para liberar (ou bloquear) a entrega do produto.
 */
@Entity
@Table(name = "conta_receber")
public class ContaReceber {

    @Id
    private UUID id;
    @Column(name = "pedido_id")
    private UUID pedidoId;
    @Column(name = "cliente_id")
    private UUID clienteId;
    @Enumerated(EnumType.STRING)
    private StatusConta status;
    @Column(name = "criada_em")
    private LocalDateTime criadaEm;
    @Column(name = "atualizada_em")
    private LocalDateTime atualizadaEm;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "conta_receber_item", joinColumns = @JoinColumn(name = "conta_id"))
    private List<ItemConta> itens = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pagamento", joinColumns = @JoinColumn(name = "conta_id"))
    @OrderBy("pagoEm")
    private List<Pagamento> pagamentos = new ArrayList<>();

    protected ContaReceber() {
    }

    public ContaReceber(UUID pedidoId, UUID clienteId, List<ItemConta> itens) {
        if (pedidoId == null || clienteId == null) {
            throw new RegraNegocioException("Pedido e cliente são obrigatórios para abrir a conta a receber.");
        }
        exigirItens(itens);
        this.id = UUID.randomUUID();
        this.pedidoId = pedidoId;
        this.clienteId = clienteId;
        this.itens.addAll(itens);
        this.status = StatusConta.ABERTA;
        this.criadaEm = LocalDateTime.now();
        this.atualizadaEm = this.criadaEm;
    }

    /**
     * Registra um recebimento. O tipo é deduzido: o primeiro pagamento parcial é o SINAL,
     * os seguintes são PARCELAS e o que zera o saldo é a QUITACAO.
     */
    public Pagamento registrarPagamento(BigDecimal valor, FormaPagamento forma, String observacao) {
        if (status == StatusConta.CANCELADA || status == StatusConta.QUITADA) {
            throw new RegraNegocioException("Não é possível registrar pagamento em conta " + status + ".");
        }
        if (valor == null || valor.signum() <= 0) {
            throw new RegraNegocioException("O valor do pagamento deve ser maior que zero.");
        }
        if (forma == null) {
            throw new RegraNegocioException("A forma de pagamento é obrigatória.");
        }
        BigDecimal saldo = saldoRemanescente();
        if (valor.compareTo(saldo) > 0) {
            throw new RegraNegocioException("O valor informado (" + valor + ") excede o saldo remanescente (" + saldo + ").");
        }
        TipoPagamento tipo = valor.compareTo(saldo) == 0 ? TipoPagamento.QUITACAO
                : pagamentos.isEmpty() ? TipoPagamento.SINAL : TipoPagamento.PARCELA;
        Pagamento pagamento = new Pagamento(valor, tipo, forma, observacao);
        pagamentos.add(pagamento);
        recalcularStatus();
        return pagamento;
    }

    /** Reflete alterações do pedido (permitidas apenas antes da produção). */
    public void atualizarItens(List<ItemConta> novosItens) {
        if (status == StatusConta.CANCELADA) {
            throw new RegraNegocioException("Conta cancelada não pode ser alterada.");
        }
        exigirItens(novosItens);
        BigDecimal novoTotal = somar(novosItens.stream().map(ItemConta::getValorVenda).toList());
        if (novoTotal.compareTo(totalPago()) < 0) {
            throw new RegraNegocioException("O novo valor do pedido (" + novoTotal + ") é menor que o total já pago ("
                    + totalPago() + "). Registre o estorno antes de alterar.");
        }
        this.itens.clear();
        this.itens.addAll(novosItens);
        recalcularStatus();
    }

    public void cancelar() {
        if (status == StatusConta.CANCELADA) {
            return;
        }
        this.status = StatusConta.CANCELADA;
        this.atualizadaEm = LocalDateTime.now();
    }

    /** Regra de liberação da entrega: só com o saldo integralmente quitado. */
    public boolean liberadaParaEntrega() {
        return status == StatusConta.QUITADA;
    }

    public BigDecimal valorTotal() {
        return somar(itens.stream().map(ItemConta::getValorVenda).toList());
    }

    public BigDecimal custoTotal() {
        return somar(itens.stream().map(ItemConta::getCustoEstimado).toList());
    }

    public BigDecimal totalPago() {
        return somar(pagamentos.stream().map(Pagamento::getValor).toList());
    }

    public BigDecimal saldoRemanescente() {
        return valorTotal().subtract(totalPago());
    }

    private void recalcularStatus() {
        BigDecimal pago = totalPago();
        if (pago.compareTo(valorTotal()) >= 0) {
            status = StatusConta.QUITADA;
        } else if (pago.signum() > 0) {
            status = StatusConta.PARCIALMENTE_PAGA;
        } else {
            status = StatusConta.ABERTA;
        }
        atualizadaEm = LocalDateTime.now();
    }

    private static void exigirItens(List<ItemConta> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new RegraNegocioException("A conta a receber precisa de ao menos um item.");
        }
    }

    private static BigDecimal somar(List<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public UUID getId() { return id; }
    public UUID getPedidoId() { return pedidoId; }
    public UUID getClienteId() { return clienteId; }
    public List<ItemConta> getItens() { return Collections.unmodifiableList(itens); }
    public List<Pagamento> getPagamentos() { return Collections.unmodifiableList(pagamentos); }
    public StatusConta getStatus() { return status; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
}
