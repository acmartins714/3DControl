package br.edu.uniesp.threedcontrol.financeiro.contareceber.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Recebimento registrado contra uma conta (sinal, parcela ou quitação). */
public record Pagamento(UUID id, BigDecimal valor, TipoPagamento tipo, FormaPagamento forma,
                        LocalDateTime pagoEm, String observacao) {

    public enum TipoPagamento { SINAL, PARCELA, QUITACAO }

    public enum FormaPagamento { PIX, DINHEIRO, CARTAO_CREDITO, CARTAO_DEBITO, TRANSFERENCIA }
}
