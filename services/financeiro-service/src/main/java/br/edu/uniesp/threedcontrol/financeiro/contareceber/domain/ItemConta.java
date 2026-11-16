package br.edu.uniesp.threedcontrol.financeiro.contareceber.domain;

import br.edu.uniesp.threedcontrol.financeiro.shared.domain.DomainException;

import java.math.BigDecimal;

/** Composição da receita de um pedido por categoria de produto (base da análise de lucratividade). */
public record ItemConta(String categoria, BigDecimal valorVenda, BigDecimal custoEstimado) {

    public ItemConta {
        if (categoria == null || categoria.isBlank()) {
            throw new DomainException("A categoria do item é obrigatória.");
        }
        if (valorVenda == null || valorVenda.signum() < 0 || custoEstimado == null || custoEstimado.signum() < 0) {
            throw new DomainException("Valor de venda e custo estimado não podem ser negativos.");
        }
    }
}
