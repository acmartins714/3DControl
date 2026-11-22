package br.edu.uniesp.threedcontrol.pedidos.rastreio;

import br.edu.uniesp.threedcontrol.pedidos.pedido.HistoricoStatus;
import br.edu.uniesp.threedcontrol.pedidos.pedido.Pedido;
import br.edu.uniesp.threedcontrol.pedidos.pedido.PedidoRepository;
import br.edu.uniesp.threedcontrol.pedidos.shared.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Consulta pública de rastreio por código, pensada para o atendimento, o site e a integração
 * automatizada do WhatsApp (SAC). Expõe apenas dados não sensíveis do pedido.
 */
@Service
@Transactional(readOnly = true)
public class RastreioService {

    public record Etapa(String status, String descricao, String dataHora, String observacao) {
    }

    public record Rastreio(String codigo, String status, String descricaoStatus, LocalDate prazoEntrega,
                           String modalidadeEntrega, boolean noPrazo, List<Etapa> linhaDoTempo) {
    }

    private final PedidoRepository pedidos;

    public RastreioService(PedidoRepository pedidos) {
        this.pedidos = pedidos;
    }

    public Rastreio rastrear(String codigo) {
        Pedido p = pedidos.findByCodigo(codigo.trim().toUpperCase())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", codigo));
        boolean noPrazo = !LocalDate.now().isAfter(p.getPrazoEntrega());
        List<Etapa> etapas = p.getHistorico().stream().map(RastreioService::etapa).toList();
        return new Rastreio(p.getCodigo(), p.getStatus().name(), p.getStatus().descricao(), p.getPrazoEntrega(),
                p.getModalidadeEntrega().name(), noPrazo, etapas);
    }

    private static Etapa etapa(HistoricoStatus h) {
        return new Etapa(h.getStatus().name(), h.getStatus().descricao(), h.getDataHora().toString(), h.getObservacao());
    }
}
