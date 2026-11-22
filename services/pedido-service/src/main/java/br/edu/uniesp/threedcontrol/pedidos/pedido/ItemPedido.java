package br.edu.uniesp.threedcontrol.pedidos.pedido;

import br.edu.uniesp.threedcontrol.pedidos.shared.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Item do pedido. Preço, nome e custo são "fotografados" no momento do pedido para que mudanças
 * posteriores no catálogo não alterem pedidos já fechados.
 */
@Embeddable
public class ItemPedido {

    @Column(name = "produto_id")
    private UUID produtoId;
    @Column(name = "nome_produto")
    private String nomeProduto;
    private String categoria;
    private int quantidade;
    @Column(name = "preco_unitario")
    private BigDecimal precoUnitario;
    @Column(name = "custo_unitario_estimado")
    private BigDecimal custoUnitarioEstimado;
    private String personalizacao;

    protected ItemPedido() {
    }

    public ItemPedido(UUID produtoId, String nomeProduto, String categoria, int quantidade,
                      BigDecimal precoUnitario, BigDecimal custoUnitarioEstimado, String personalizacao) {
        if (produtoId == null) {
            throw new RegraNegocioException("O produto do item é obrigatório.");
        }
        if (quantidade <= 0) {
            throw new RegraNegocioException("A quantidade do item deve ser maior que zero.");
        }
        if (precoUnitario == null || precoUnitario.signum() < 0) {
            throw new RegraNegocioException("O preço unitário não pode ser negativo.");
        }
        this.produtoId = produtoId;
        this.nomeProduto = nomeProduto;
        this.categoria = categoria;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.custoUnitarioEstimado = custoUnitarioEstimado == null ? BigDecimal.ZERO : custoUnitarioEstimado;
        this.personalizacao = personalizacao;
    }

    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal custoTotalEstimado() {
        return custoUnitarioEstimado.multiply(BigDecimal.valueOf(quantidade));
    }

    public UUID getProdutoId() { return produtoId; }
    public String getNomeProduto() { return nomeProduto; }
    public String getCategoria() { return categoria; }
    public int getQuantidade() { return quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public BigDecimal getCustoUnitarioEstimado() { return custoUnitarioEstimado; }
    public String getPersonalizacao() { return personalizacao; }
}
