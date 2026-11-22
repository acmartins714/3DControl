package br.edu.uniesp.threedcontrol.financeiro.contareceber;

import br.edu.uniesp.threedcontrol.financeiro.shared.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

/** Composição da receita de um pedido por categoria de produto (base da análise de lucratividade). */
@Embeddable
public class ItemConta {

    private String categoria;
    @Column(name = "valor_venda")
    private BigDecimal valorVenda;
    @Column(name = "custo_estimado")
    private BigDecimal custoEstimado;

    protected ItemConta() {
    }

    public ItemConta(String categoria, BigDecimal valorVenda, BigDecimal custoEstimado) {
        if (categoria == null || categoria.isBlank()) {
            throw new RegraNegocioException("A categoria do item é obrigatória.");
        }
        if (valorVenda == null || valorVenda.signum() < 0 || custoEstimado == null || custoEstimado.signum() < 0) {
            throw new RegraNegocioException("Valor de venda e custo estimado não podem ser negativos.");
        }
        this.categoria = categoria;
        this.valorVenda = valorVenda;
        this.custoEstimado = custoEstimado;
    }

    public String getCategoria() { return categoria; }
    public BigDecimal getValorVenda() { return valorVenda; }
    public BigDecimal getCustoEstimado() { return custoEstimado; }
}
