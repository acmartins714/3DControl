package br.edu.uniesp.threedcontrol.pedidos.rastreio.application;

import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.HistoricoStatus;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.Pedido;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.PedidoRepository;
import br.edu.uniesp.threedcontrol.pedidos.shared.domain.RecursoNaoEncontradoException;

import java.time.LocalDate;
import java.util.List;

/**
 * Consulta pública de rastreio por código — pensada para o atendimento, o site e a integração
 * automatizada do WhatsApp (SAC). Expõe apenas dados não sensíveis do pedido.
 */
public class RastrearPedidoUseCase {

    public record Etapa(String status, String descricao, String dataHora, String observacao) {
    }

    public record Rastreio(String codigo, String status, String descricaoStatus, LocalDate prazoEntrega,
                           String modalidadeEntrega, boolean noPrazo, List<Etapa> linhaDoTempo) {
    }

    private final PedidoRepository pedidos;

    public RastrearPedidoUseCase(PedidoRepository pedidos) {
        this.pedidos = pedidos;
    }

    public Rastreio rastrear(String codigo) {
        Pedido p = pedidos.buscarPorCodigo(codigo.trim().toUpperCase())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", codigo));
        boolean noPrazo = !LocalDate.now().isAfter(p.getPrazoEntrega());
        List<Etapa> etapas = p.getHistorico().stream().map(RastrearPedidoUseCase::etapa).toList();
        return new Rastreio(p.getCodigo(), p.getStatus().name(), p.getStatus().descricao(), p.getPrazoEntrega(),
                p.getModalidadeEntrega().name(), noPrazo, etapas);
    }

    private static Etapa etapa(HistoricoStatus h) {
        return new Etapa(h.status().name(), h.status().descricao(), h.dataHora().toString(), h.observacao());
    }
}
