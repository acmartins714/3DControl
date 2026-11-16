package br.edu.uniesp.threedcontrol.pedidos.pedido.application;

import org.springframework.stereotype.Component;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.PedidoCommands.ItemCommand;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.CadastroGateway;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.CadastroGateway.InsumoNecessario;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.CadastroGateway.ProdutoCatalogo;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.FinanceiroGateway.ItemFinanceiro;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.ItemPedido;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.Pedido;
import br.edu.uniesp.threedcontrol.pedidos.shared.domain.DomainException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Componente de aplicação que resolve os itens do pedido no catálogo (preço, custo, categoria)
 * e calcula a necessidade total de insumos (ficha técnica × quantidade).
 */
@Component
public class MontadorItensPedido {

    public record ItensResolvidos(List<ItemPedido> itens, List<InsumoNecessario> necessidades) {
    }

    private final CadastroGateway cadastro;

    public MontadorItensPedido(CadastroGateway cadastro) {
        this.cadastro = cadastro;
    }

    public ItensResolvidos montar(List<ItemCommand> comandos) {
        if (comandos == null || comandos.isEmpty()) {
            throw new DomainException("O pedido deve ter ao menos um item.");
        }
        List<ItemPedido> itens = new ArrayList<>();
        List<InsumoNecessario> necessidades = new ArrayList<>();
        for (ItemCommand cmd : comandos) {
            ProdutoCatalogo produto = produtoAtivo(cmd.produtoId());
            BigDecimal preco = cmd.precoUnitario() != null ? cmd.precoUnitario() : produto.precoBase();
            itens.add(new ItemPedido(produto.id(), produto.nome(), produto.categoria(), cmd.quantidade(), preco,
                    produto.custoEstimado(), cmd.personalizacao()));
            necessidades.addAll(multiplicar(produto, cmd.quantidade()));
        }
        return new ItensResolvidos(itens, necessidades);
    }

    /** Recalcula as necessidades de insumo de itens já registrados (ex.: reprocessamento). */
    public List<InsumoNecessario> necessidades(List<ItemPedido> itens) {
        List<InsumoNecessario> necessidades = new ArrayList<>();
        for (ItemPedido item : itens) {
            ProdutoCatalogo produto = cadastro.buscarProduto(item.produtoId())
                    .orElseThrow(() -> new DomainException("Produto não encontrado no catálogo: " + item.produtoId()));
            necessidades.addAll(multiplicar(produto, item.quantidade()));
        }
        return necessidades;
    }

    public static List<ItemFinanceiro> itensFinanceiros(Pedido pedido) {
        return pedido.getItens().stream()
                .map(i -> new ItemFinanceiro(i.categoria(), i.subtotal(), i.custoTotalEstimado()))
                .toList();
    }

    private ProdutoCatalogo produtoAtivo(UUID produtoId) {
        ProdutoCatalogo produto = cadastro.buscarProduto(produtoId)
                .orElseThrow(() -> new DomainException("Produto não encontrado no catálogo: " + produtoId));
        if (!produto.ativo()) {
            throw new DomainException("O produto '" + produto.nome() + "' está inativo no catálogo.");
        }
        return produto;
    }

    private static List<InsumoNecessario> multiplicar(ProdutoCatalogo produto, int quantidade) {
        return produto.composicao().stream()
                .map(c -> new InsumoNecessario(c.insumoId(), c.quantidade().multiply(BigDecimal.valueOf(quantidade))))
                .toList();
    }
}
