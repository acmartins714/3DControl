package br.edu.uniesp.threedcontrol.pedidos.pedido.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.MontadorItensPedido.ItensResolvidos;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.PedidoCommands.NovoPedido;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.CadastroGateway;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.EstoqueGateway;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.FinanceiroGateway;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.ProducaoGateway;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.Pedido;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.PedidoRepository;
import br.edu.uniesp.threedcontrol.pedidos.shared.domain.DomainException;

/**
 * Registro de um pedido vindo de QUALQUER canal (WhatsApp, Instagram, Facebook, telefone, site, balcão).
 * <p>
 * Orquestra o fluxo único: valida cliente e produtos no cadastro → reserva insumos no estoque
 * (ou sinaliza a compra) → inclui a ordem na fila de produção → abre a conta a receber.
 * Em caso de falha após efeitos remotos, executa compensações (saga orquestrada simples).
 */
@Service
@Transactional
public class RegistrarPedidoUseCase {

    private final PedidoRepository pedidos;
    private final CadastroGateway cadastro;
    private final MontadorItensPedido montador;
    private final EstoqueGateway estoque;
    private final ProducaoGateway producao;
    private final FinanceiroGateway financeiro;

    public RegistrarPedidoUseCase(PedidoRepository pedidos, CadastroGateway cadastro, MontadorItensPedido montador,
                                  EstoqueGateway estoque, ProducaoGateway producao, FinanceiroGateway financeiro) {
        this.pedidos = pedidos;
        this.cadastro = cadastro;
        this.montador = montador;
        this.estoque = estoque;
        this.producao = producao;
        this.financeiro = financeiro;
    }

    public Pedido executar(NovoPedido cmd) {
        var cliente = cadastro.buscarCliente(cmd.clienteId())
                .orElseThrow(() -> new DomainException("Cliente não encontrado: " + cmd.clienteId()));
        if (!cliente.ativo()) {
            throw new DomainException("O cliente '" + cliente.nome() + "' está inativo.");
        }

        ItensResolvidos resolvidos = montador.montar(cmd.itens());
        Pedido pedido = Pedido.registrar(cmd.clienteId(), cmd.canalOrigem(), cmd.atendenteResponsavel(),
                cmd.prazoEntrega(), cmd.modalidadeEntrega(), cmd.condicaoPagamento(), cmd.observacoes(),
                resolvidos.itens());

        EstoqueGateway.ResultadoReserva reserva = estoque.reservar(pedido.getId(), resolvidos.necessidades());
        try {
            pedido.atualizarDisponibilidadeInsumos(reserva.reservado(),
                    "Aguardando compra de: " + String.join(", ", reserva.insumosFaltantes()));
            producao.planejarOrdem(pedido.getId(), pedido.getCodigo(), pedido.getPrazoEntrega(), reserva.reservado());
            financeiro.abrirConta(pedido.getId(), pedido.getClienteId(), MontadorItensPedido.itensFinanceiros(pedido));
            return pedidos.salvar(pedido);
        } catch (RuntimeException e) {
            compensar(pedido);
            throw e;
        }
    }

    private void compensar(Pedido pedido) {
        executarSilenciosamente(() -> estoque.liberarReservas(pedido.getId()));
        executarSilenciosamente(() -> producao.cancelarOrdem(pedido.getId()));
        executarSilenciosamente(() -> financeiro.cancelarConta(pedido.getId()));
    }

    private static void executarSilenciosamente(Runnable acao) {
        try {
            acao.run();
        } catch (RuntimeException ignorada) {
            // compensação best-effort: a falha original é a que deve ser reportada
        }
    }
}
