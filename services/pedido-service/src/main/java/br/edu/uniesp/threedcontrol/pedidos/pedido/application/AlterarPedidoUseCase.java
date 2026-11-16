package br.edu.uniesp.threedcontrol.pedidos.pedido.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.MontadorItensPedido.ItensResolvidos;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.PedidoCommands.AlteracaoPedido;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.EstoqueGateway;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.FinanceiroGateway;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.ProducaoGateway;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.Pedido;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.PedidoRepository;
import br.edu.uniesp.threedcontrol.pedidos.shared.domain.RecursoNaoEncontradoException;

import java.util.UUID;

/**
 * Alteração de pedido solicitada pelo cliente (ex.: mudança via WhatsApp).
 * A Regra de Ouro é verificada ANTES de qualquer efeito colateral; depois a alteração é propagada
 * ao estoque (nova reserva), à produção (replanejamento) e ao financeiro (novo valor).
 */
@Service
@Transactional
public class AlterarPedidoUseCase {

    private final PedidoRepository pedidos;
    private final MontadorItensPedido montador;
    private final EstoqueGateway estoque;
    private final ProducaoGateway producao;
    private final FinanceiroGateway financeiro;

    public AlterarPedidoUseCase(PedidoRepository pedidos, MontadorItensPedido montador, EstoqueGateway estoque,
                                ProducaoGateway producao, FinanceiroGateway financeiro) {
        this.pedidos = pedidos;
        this.montador = montador;
        this.estoque = estoque;
        this.producao = producao;
        this.financeiro = financeiro;
    }

    public Pedido executar(UUID pedidoId, AlteracaoPedido cmd) {
        Pedido pedido = pedidos.buscarPorId(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", pedidoId));
        pedido.garantirAlteravel();

        ItensResolvidos resolvidos = montador.montar(cmd.itens());
        pedido.alterar(cmd.prazoEntrega(), cmd.modalidadeEntrega(), cmd.condicaoPagamento(), cmd.observacoes(),
                resolvidos.itens());

        EstoqueGateway.ResultadoReserva reserva = estoque.reservar(pedido.getId(), resolvidos.necessidades());
        pedido.atualizarDisponibilidadeInsumos(reserva.reservado(),
                "Aguardando compra de: " + String.join(", ", reserva.insumosFaltantes()));
        producao.planejarOrdem(pedido.getId(), pedido.getCodigo(), pedido.getPrazoEntrega(), reserva.reservado());
        financeiro.atualizarConta(pedido.getId(), MontadorItensPedido.itensFinanceiros(pedido));
        return pedidos.salvar(pedido);
    }
}
