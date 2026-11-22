package br.edu.uniesp.threedcontrol.pedidos.pedido;

import br.edu.uniesp.threedcontrol.pedidos.pedido.PedidoDtos.AlteracaoRequest;
import br.edu.uniesp.threedcontrol.pedidos.pedido.PedidoDtos.ItemRequest;
import br.edu.uniesp.threedcontrol.pedidos.pedido.PedidoDtos.NovoPedidoRequest;
import br.edu.uniesp.threedcontrol.pedidos.pedido.PedidoDtos.PedidoResponse;
import br.edu.uniesp.threedcontrol.pedidos.pedido.integracao.CadastroClient;
import br.edu.uniesp.threedcontrol.pedidos.pedido.integracao.EstoqueClient;
import br.edu.uniesp.threedcontrol.pedidos.pedido.integracao.EstoqueClient.InsumoNecessario;
import br.edu.uniesp.threedcontrol.pedidos.pedido.integracao.EstoqueClient.ResultadoReserva;
import br.edu.uniesp.threedcontrol.pedidos.pedido.integracao.FinanceiroClient;
import br.edu.uniesp.threedcontrol.pedidos.pedido.integracao.FinanceiroClient.ItemFinanceiro;
import br.edu.uniesp.threedcontrol.pedidos.pedido.integracao.FinanceiroClient.SituacaoPagamento;
import br.edu.uniesp.threedcontrol.pedidos.pedido.integracao.ProducaoClient;
import br.edu.uniesp.threedcontrol.pedidos.shared.RecursoNaoEncontradoException;
import br.edu.uniesp.threedcontrol.pedidos.shared.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orquestra o ciclo de vida do pedido e a integração com os demais serviços.
 * <p>
 * Registro (qualquer canal): valida cliente e produtos no cadastro → reserva insumos no estoque
 * (ou sinaliza a compra) → inclui a ordem na fila de produção → abre a conta a receber.
 * Se uma etapa falhar depois de efeitos em outros serviços, executa compensações (saga orquestrada).
 */
@Service
@Transactional
public class PedidoService {

    /** Itens do pedido já resolvidos no catálogo e a necessidade total de insumos que eles geram. */
    private record ItensResolvidos(List<ItemPedido> itens, List<InsumoNecessario> necessidades) {
    }

    private final PedidoRepository pedidos;
    private final CadastroClient cadastro;
    private final EstoqueClient estoque;
    private final ProducaoClient producao;
    private final FinanceiroClient financeiro;

    public PedidoService(PedidoRepository pedidos, CadastroClient cadastro, EstoqueClient estoque,
                         ProducaoClient producao, FinanceiroClient financeiro) {
        this.pedidos = pedidos;
        this.cadastro = cadastro;
        this.estoque = estoque;
        this.producao = producao;
        this.financeiro = financeiro;
    }

    // ------------------------------------------------------------------ Registro e alteração

    public PedidoResponse registrar(NovoPedidoRequest req) {
        CadastroClient.Cliente cliente = cadastro.buscarCliente(req.clienteId())
                .orElseThrow(() -> new RegraNegocioException("Cliente não encontrado: " + req.clienteId()));
        if (!cliente.ativo()) {
            throw new RegraNegocioException("O cliente '" + cliente.nome() + "' está inativo.");
        }
        ItensResolvidos resolvidos = resolverItens(req.itens());
        Pedido pedido = new Pedido(req.clienteId(), req.canalOrigem(), req.atendenteResponsavel(),
                req.prazoEntrega(), req.modalidadeEntrega(), req.condicaoPagamento(), req.observacoes(),
                resolvidos.itens());

        ResultadoReserva reserva = estoque.reservar(pedido.getId(), resolvidos.necessidades());
        try {
            aplicarReserva(pedido, reserva);
            financeiro.abrirConta(pedido.getId(), pedido.getClienteId(), itensFinanceiros(pedido));
            return PedidoResponse.from(pedidos.save(pedido));
        } catch (RuntimeException e) {
            compensar(pedido.getId());
            throw e;
        }
    }

    /**
     * Alteração solicitada pelo cliente (ex.: mudança via WhatsApp). A Regra de Ouro é verificada
     * ANTES de qualquer efeito nos outros serviços.
     */
    public PedidoResponse alterar(UUID pedidoId, AlteracaoRequest req) {
        Pedido pedido = buscarEntidade(pedidoId);
        pedido.garantirAlteravel();
        ItensResolvidos resolvidos = resolverItens(req.itens());
        pedido.alterar(req.prazoEntrega(), req.modalidadeEntrega(), req.condicaoPagamento(), req.observacoes(),
                resolvidos.itens());
        aplicarReserva(pedido, estoque.reservar(pedido.getId(), resolvidos.necessidades()));
        financeiro.atualizarConta(pedido.getId(), itensFinanceiros(pedido));
        return PedidoResponse.from(pedido);
    }

    // ------------------------------------------------------------------ Ciclo de vida

    /** Tenta novamente reservar os insumos (ex.: após a chegada de uma compra). */
    public PedidoResponse reprocessarInsumos(UUID pedidoId) {
        Pedido pedido = buscarEntidade(pedidoId);
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_INSUMO) {
            throw new RegraNegocioException("O pedido " + pedido.getCodigo() + " não está aguardando insumos.");
        }
        aplicarReserva(pedido, estoque.reservar(pedido.getId(), necessidades(pedido.getItens())));
        return PedidoResponse.from(pedido);
    }

    /** Acionado pelo producao-service. A partir daqui o pedido fica bloqueado para alterações. */
    public PedidoResponse iniciarProducao(UUID pedidoId) {
        Pedido pedido = buscarEntidade(pedidoId);
        pedido.iniciarProducao();
        estoque.consumirReservas(pedido.getId());
        return PedidoResponse.from(pedido);
    }

    /** Acionado pelo producao-service. */
    public PedidoResponse finalizarProducao(UUID pedidoId) {
        Pedido pedido = buscarEntidade(pedidoId);
        pedido.finalizarProducao();
        return PedidoResponse.from(pedido);
    }

    public PedidoResponse despachar(UUID pedidoId) {
        Pedido pedido = buscarEntidade(pedidoId);
        pedido.despachar();
        return PedidoResponse.from(pedido);
    }

    /** Validação na entrega: consulta o financeiro e só libera com o saldo quitado. */
    public PedidoResponse confirmarEntrega(UUID pedidoId) {
        Pedido pedido = buscarEntidade(pedidoId);
        SituacaoPagamento situacao = financeiro.consultarSituacao(pedidoId);
        pedido.confirmarEntrega(situacao.quitado(), situacao.saldoRemanescente());
        return PedidoResponse.from(pedido);
    }

    public PedidoResponse cancelar(UUID pedidoId, String motivo) {
        Pedido pedido = buscarEntidade(pedidoId);
        pedido.cancelar(motivo);
        estoque.liberarReservas(pedidoId);
        producao.cancelarOrdem(pedidoId);
        financeiro.cancelarConta(pedidoId);
        return PedidoResponse.from(pedido);
    }

    // ------------------------------------------------------------------ Consultas

    @Transactional(readOnly = true)
    public PedidoResponse buscar(UUID id) {
        return PedidoResponse.from(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(StatusPedido status, UUID clienteId) {
        List<Pedido> lista;
        if (status != null && clienteId != null) {
            lista = pedidos.findByStatusAndClienteIdOrderByPrazoEntrega(status, clienteId);
        } else if (status != null) {
            lista = pedidos.findByStatusOrderByPrazoEntrega(status);
        } else if (clienteId != null) {
            lista = pedidos.findByClienteIdOrderByCriadoEmDesc(clienteId);
        } else {
            lista = pedidos.findAllByOrderByPrazoEntrega();
        }
        return lista.stream().map(PedidoResponse::from).toList();
    }

    // ------------------------------------------------------------------ Auxiliares

    private Pedido buscarEntidade(UUID id) {
        return pedidos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
    }

    /** Atualiza o status do pedido com o resultado da reserva e inclui/replaneja a ordem de produção. */
    private void aplicarReserva(Pedido pedido, ResultadoReserva reserva) {
        pedido.atualizarDisponibilidadeInsumos(reserva.reservado(),
                "Aguardando compra de: " + String.join(", ", reserva.insumosFaltantes()));
        producao.planejarOrdem(pedido.getId(), pedido.getCodigo(), pedido.getPrazoEntrega(), reserva.reservado());
    }

    /** Resolve os itens no catálogo (preço, custo, categoria) e calcula a necessidade de insumos. */
    private ItensResolvidos resolverItens(List<ItemRequest> itensRequest) {
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new RegraNegocioException("O pedido deve ter ao menos um item.");
        }
        List<ItemPedido> itens = new ArrayList<>();
        List<InsumoNecessario> necessidades = new ArrayList<>();
        for (ItemRequest req : itensRequest) {
            CadastroClient.Produto produto = buscarProduto(req.produtoId());
            if (!produto.ativo()) {
                throw new RegraNegocioException("O produto '" + produto.nome() + "' está inativo no catálogo.");
            }
            BigDecimal preco = req.precoUnitario() != null ? req.precoUnitario() : produto.precoBase();
            itens.add(new ItemPedido(produto.id(), produto.nome(), produto.categoria(), req.quantidade(), preco,
                    produto.custoEstimado(), req.personalizacao()));
            necessidades.addAll(multiplicar(produto, req.quantidade()));
        }
        return new ItensResolvidos(itens, necessidades);
    }

    /** Recalcula as necessidades de insumo de itens já registrados (ex.: reprocessamento). */
    private List<InsumoNecessario> necessidades(List<ItemPedido> itens) {
        List<InsumoNecessario> necessidades = new ArrayList<>();
        for (ItemPedido item : itens) {
            necessidades.addAll(multiplicar(buscarProduto(item.getProdutoId()), item.getQuantidade()));
        }
        return necessidades;
    }

    private CadastroClient.Produto buscarProduto(UUID produtoId) {
        return cadastro.buscarProduto(produtoId)
                .orElseThrow(() -> new RegraNegocioException("Produto não encontrado no catálogo: " + produtoId));
    }

    private static List<InsumoNecessario> multiplicar(CadastroClient.Produto produto, int quantidade) {
        return produto.composicao().stream()
                .map(c -> new InsumoNecessario(c.insumoId(), c.quantidade().multiply(BigDecimal.valueOf(quantidade))))
                .toList();
    }

    private static List<ItemFinanceiro> itensFinanceiros(Pedido pedido) {
        return pedido.getItens().stream()
                .map(i -> new ItemFinanceiro(i.getCategoria(), i.subtotal(), i.custoTotalEstimado()))
                .toList();
    }

    /** Desfaz os efeitos já aplicados nos outros serviços (best-effort: a falha original é a reportada). */
    private void compensar(UUID pedidoId) {
        executarSilenciosamente(() -> estoque.liberarReservas(pedidoId));
        executarSilenciosamente(() -> producao.cancelarOrdem(pedidoId));
        executarSilenciosamente(() -> financeiro.cancelarConta(pedidoId));
    }

    private static void executarSilenciosamente(Runnable acao) {
        try {
            acao.run();
        } catch (RuntimeException ignorada) {
            // compensação best-effort
        }
    }
}
