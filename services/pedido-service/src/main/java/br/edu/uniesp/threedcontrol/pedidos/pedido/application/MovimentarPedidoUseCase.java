package br.edu.uniesp.threedcontrol.pedidos.pedido.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.EstoqueGateway;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.FinanceiroGateway;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.ProducaoGateway;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.Pedido;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.PedidoRepository;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.StatusPedido;
import br.edu.uniesp.threedcontrol.pedidos.shared.domain.DomainException;
import br.edu.uniesp.threedcontrol.pedidos.shared.domain.RecursoNaoEncontradoException;

import java.util.UUID;

/** Transições do ciclo de vida do pedido após o registro. */
@Service
@Transactional
public class MovimentarPedidoUseCase {

    private final PedidoRepository pedidos;
    private final MontadorItensPedido montador;
    private final EstoqueGateway estoque;
    private final ProducaoGateway producao;
    private final FinanceiroGateway financeiro;

    public MovimentarPedidoUseCase(PedidoRepository pedidos, MontadorItensPedido montador, EstoqueGateway estoque,
                                   ProducaoGateway producao, FinanceiroGateway financeiro) {
        this.pedidos = pedidos;
        this.montador = montador;
        this.estoque = estoque;
        this.producao = producao;
        this.financeiro = financeiro;
    }

    /** Tenta novamente reservar os insumos (ex.: após a chegada de uma compra). */
    public Pedido reprocessarInsumos(UUID pedidoId) {
        Pedido pedido = buscar(pedidoId);
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_INSUMO) {
            throw new DomainException("O pedido " + pedido.getCodigo() + " não está aguardando insumos.");
        }
        var reserva = estoque.reservar(pedido.getId(), montador.necessidades(pedido.getItens()));
        pedido.atualizarDisponibilidadeInsumos(reserva.reservado(),
                "Aguardando compra de: " + String.join(", ", reserva.insumosFaltantes()));
        producao.planejarOrdem(pedido.getId(), pedido.getCodigo(), pedido.getPrazoEntrega(), reserva.reservado());
        return pedidos.salvar(pedido);
    }

    /** Acionado pelo producao-service. A partir daqui o pedido fica bloqueado para alterações. */
    public Pedido iniciarProducao(UUID pedidoId) {
        Pedido pedido = buscar(pedidoId);
        pedido.iniciarProducao();
        estoque.consumirReservas(pedido.getId());
        return pedidos.salvar(pedido);
    }

    /** Acionado pelo producao-service. */
    public Pedido finalizarProducao(UUID pedidoId) {
        Pedido pedido = buscar(pedidoId);
        pedido.finalizarProducao();
        return pedidos.salvar(pedido);
    }

    public Pedido despachar(UUID pedidoId) {
        Pedido pedido = buscar(pedidoId);
        pedido.despachar();
        return pedidos.salvar(pedido);
    }

    /** Validação na entrega: consulta o financeiro e só libera com o saldo quitado. */
    public Pedido confirmarEntrega(UUID pedidoId) {
        Pedido pedido = buscar(pedidoId);
        FinanceiroGateway.SituacaoPagamento situacao = financeiro.consultarSituacao(pedidoId);
        pedido.confirmarEntrega(situacao.quitado(), situacao.saldoRemanescente());
        return pedidos.salvar(pedido);
    }

    public Pedido cancelar(UUID pedidoId, String motivo) {
        Pedido pedido = buscar(pedidoId);
        pedido.cancelar(motivo);
        estoque.liberarReservas(pedidoId);
        producao.cancelarOrdem(pedidoId);
        financeiro.cancelarConta(pedidoId);
        return pedidos.salvar(pedido);
    }

    private Pedido buscar(UUID id) {
        return pedidos.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
    }
}
