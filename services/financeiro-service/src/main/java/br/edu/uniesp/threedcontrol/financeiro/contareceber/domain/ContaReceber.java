package br.edu.uniesp.threedcontrol.financeiro.contareceber.domain;

import br.edu.uniesp.threedcontrol.financeiro.contareceber.domain.Pagamento.FormaPagamento;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.domain.Pagamento.TipoPagamento;
import br.edu.uniesp.threedcontrol.financeiro.shared.domain.DomainException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Agregado que controla quanto o cliente deve por um pedido e o que já foi pago.
 * É a fonte da verdade para liberar (ou bloquear) a entrega do produto.
 */
public class ContaReceber {

    private final UUID id;
    private final UUID pedidoId;
    private final UUID clienteId;
    private List<ItemConta> itens;
    private final List<Pagamento> pagamentos;
    private StatusConta status;
    private final LocalDateTime criadaEm;
    private LocalDateTime atualizadaEm;

    private ContaReceber(UUID id, UUID pedidoId, UUID clienteId, List<ItemConta> itens, List<Pagamento> pagamentos,
                         StatusConta status, LocalDateTime criadaEm, LocalDateTime atualizadaEm) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.clienteId = clienteId;
        this.itens = itens;
        this.pagamentos = pagamentos;
        this.status = status;
        this.criadaEm = criadaEm;
        this.atualizadaEm = atualizadaEm;
    }

    public static ContaReceber abrir(UUID pedidoId, UUID clienteId, List<ItemConta> itens) {
        if (pedidoId == null || clienteId == null) {
            throw new DomainException("Pedido e cliente são obrigatórios para abrir a conta a receber.");
        }
        if (itens == null || itens.isEmpty()) {
            throw new DomainException("A conta a receber precisa de ao menos um item.");
        }
        LocalDateTime agora = LocalDateTime.now();
        return new ContaReceber(UUID.randomUUID(), pedidoId, clienteId, List.copyOf(itens), new ArrayList<>(),
                StatusConta.ABERTA, agora, agora);
    }

    public static ContaReceber restaurar(UUID id, UUID pedidoId, UUID clienteId, List<ItemConta> itens,
                                         List<Pagamento> pagamentos, StatusConta status,
                                         LocalDateTime criadaEm, LocalDateTime atualizadaEm) {
        return new ContaReceber(id, pedidoId, clienteId, List.copyOf(itens), new ArrayList<>(pagamentos),
                status, criadaEm, atualizadaEm);
    }

    /**
     * Registra um recebimento. O tipo é deduzido: o primeiro pagamento parcial é o SINAL,
     * os seguintes são PARCELAS e o que zera o saldo é a QUITACAO.
     */
    public Pagamento registrarPagamento(BigDecimal valor, FormaPagamento forma, String observacao) {
        if (status == StatusConta.CANCELADA || status == StatusConta.QUITADA) {
            throw new DomainException("Não é possível registrar pagamento em conta " + status + ".");
        }
        if (valor == null || valor.signum() <= 0) {
            throw new DomainException("O valor do pagamento deve ser maior que zero.");
        }
        if (forma == null) {
            throw new DomainException("A forma de pagamento é obrigatória.");
        }
        BigDecimal saldo = saldoRemanescente();
        if (valor.compareTo(saldo) > 0) {
            throw new DomainException("O valor informado (" + valor + ") excede o saldo remanescente (" + saldo + ").");
        }
        TipoPagamento tipo = valor.compareTo(saldo) == 0 ? TipoPagamento.QUITACAO
                : pagamentos.isEmpty() ? TipoPagamento.SINAL : TipoPagamento.PARCELA;
        Pagamento pagamento = new Pagamento(UUID.randomUUID(), valor, tipo, forma, LocalDateTime.now(), observacao);
        pagamentos.add(pagamento);
        recalcularStatus();
        return pagamento;
    }

    /** Reflete alterações do pedido (permitidas apenas antes da produção). */
    public void atualizarItens(List<ItemConta> novosItens) {
        if (status == StatusConta.CANCELADA) {
            throw new DomainException("Conta cancelada não pode ser alterada.");
        }
        if (novosItens == null || novosItens.isEmpty()) {
            throw new DomainException("A conta a receber precisa de ao menos um item.");
        }
        BigDecimal novoTotal = somar(novosItens.stream().map(ItemConta::valorVenda).toList());
        if (novoTotal.compareTo(totalPago()) < 0) {
            throw new DomainException("O novo valor do pedido (" + novoTotal + ") é menor que o total já pago ("
                    + totalPago() + "). Registre o estorno antes de alterar.");
        }
        this.itens = List.copyOf(novosItens);
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
        return somar(itens.stream().map(ItemConta::valorVenda).toList());
    }

    public BigDecimal custoTotal() {
        return somar(itens.stream().map(ItemConta::custoEstimado).toList());
    }

    public BigDecimal totalPago() {
        return somar(pagamentos.stream().map(Pagamento::valor).toList());
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

    private static BigDecimal somar(List<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public UUID getId() { return id; }
    public UUID getPedidoId() { return pedidoId; }
    public UUID getClienteId() { return clienteId; }
    public List<ItemConta> getItens() { return itens; }
    public List<Pagamento> getPagamentos() { return Collections.unmodifiableList(pagamentos); }
    public StatusConta getStatus() { return status; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
}
