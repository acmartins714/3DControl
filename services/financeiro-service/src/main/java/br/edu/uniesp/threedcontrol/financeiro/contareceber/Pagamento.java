package br.edu.uniesp.threedcontrol.financeiro.contareceber;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Recebimento registrado contra uma conta (sinal, parcela ou quitação). */
@Embeddable
public class Pagamento {

    public enum TipoPagamento { SINAL, PARCELA, QUITACAO }

    public enum FormaPagamento { PIX, DINHEIRO, CARTAO_CREDITO, CARTAO_DEBITO, TRANSFERENCIA }

    @Column(name = "pagamento_id")
    private UUID id;
    private BigDecimal valor;
    @Enumerated(EnumType.STRING)
    private TipoPagamento tipo;
    @Enumerated(EnumType.STRING)
    private FormaPagamento forma;
    @Column(name = "pago_em")
    private LocalDateTime pagoEm;
    private String observacao;

    protected Pagamento() {
    }

    Pagamento(BigDecimal valor, TipoPagamento tipo, FormaPagamento forma, String observacao) {
        this.id = UUID.randomUUID();
        this.valor = valor;
        this.tipo = tipo;
        this.forma = forma;
        this.pagoEm = LocalDateTime.now();
        this.observacao = observacao;
    }

    public UUID getId() { return id; }
    public BigDecimal getValor() { return valor; }
    public TipoPagamento getTipo() { return tipo; }
    public FormaPagamento getForma() { return forma; }
    public LocalDateTime getPagoEm() { return pagoEm; }
    public String getObservacao() { return observacao; }
}
