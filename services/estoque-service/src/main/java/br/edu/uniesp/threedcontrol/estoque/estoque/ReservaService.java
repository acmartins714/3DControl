package br.edu.uniesp.threedcontrol.estoque.estoque;

import br.edu.uniesp.threedcontrol.estoque.estoque.EstoqueDtos.InsumoFaltante;
import br.edu.uniesp.threedcontrol.estoque.estoque.EstoqueDtos.NecessidadeRequest;
import br.edu.uniesp.threedcontrol.estoque.estoque.EstoqueDtos.ResultadoReserva;
import br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.SolicitacaoCompra;
import br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.SolicitacaoCompraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Verifica a disponibilidade dos insumos de um pedido e, se houver saldo para TODOS, reserva-os.
 * Caso contrário, nada é reservado e uma solicitação de compra é aberta para cada insumo faltante,
 * sinalizando à administração a demanda ANTES do início da fabricação.
 * <p>
 * A operação é idempotente por pedido: reservas ativas anteriores são liberadas antes de reservar de novo
 * (necessário quando o pedido é alterado).
 */
@Service
@Transactional
public class ReservaService {

    private final ItemEstoqueRepository itens;
    private final ReservaRepository reservas;
    private final MovimentacaoEstoqueRepository movimentacoes;
    private final SolicitacaoCompraRepository solicitacoes;

    public ReservaService(ItemEstoqueRepository itens, ReservaRepository reservas,
                          MovimentacaoEstoqueRepository movimentacoes, SolicitacaoCompraRepository solicitacoes) {
        this.itens = itens;
        this.reservas = reservas;
        this.movimentacoes = movimentacoes;
        this.solicitacoes = solicitacoes;
    }

    public ResultadoReserva reservar(UUID pedidoId, List<NecessidadeRequest> necessidades) {
        liberar(pedidoId);

        Map<UUID, BigDecimal> totalPorInsumo = necessidades.stream()
                .collect(Collectors.toMap(NecessidadeRequest::insumoId, NecessidadeRequest::quantidade,
                        BigDecimal::add, LinkedHashMap::new));
        Map<UUID, ItemEstoque> estoque = itensPorInsumo(totalPorInsumo.keySet());

        List<InsumoFaltante> faltantes = new ArrayList<>();
        totalPorInsumo.forEach((insumoId, necessario) -> {
            ItemEstoque item = estoque.get(insumoId);
            BigDecimal livre = item == null ? BigDecimal.ZERO : item.saldoLivre().max(BigDecimal.ZERO);
            if (livre.compareTo(necessario) < 0) {
                String nome = item == null ? "Insumo sem controle de estoque" : item.getNomeInsumo();
                faltantes.add(new InsumoFaltante(insumoId, nome, necessario, livre, necessario.subtract(livre)));
            }
        });

        if (!faltantes.isEmpty()) {
            solicitarCompra(pedidoId, faltantes);
            return new ResultadoReserva(pedidoId, false, faltantes);
        }

        totalPorInsumo.forEach((insumoId, quantidade) -> {
            estoque.get(insumoId).reservar(quantidade);
            reservas.save(new Reserva(pedidoId, insumoId, quantidade));
            movimentacoes.save(new MovimentacaoEstoque(insumoId, MovimentacaoEstoque.Tipo.RESERVA,
                    quantidade, pedidoId, null));
        });
        return new ResultadoReserva(pedidoId, true, List.of());
    }

    /** Baixa definitiva das reservas quando o pedido entra em produção. */
    public void consumir(UUID pedidoId) {
        aplicar(pedidoId, (item, reserva) -> {
            item.consumirReserva(reserva.getQuantidade());
            reserva.consumir();
            return MovimentacaoEstoque.Tipo.CONSUMO;
        });
    }

    /** Devolve ao saldo livre as quantidades reservadas (alteração ou cancelamento do pedido). */
    public void liberar(UUID pedidoId) {
        aplicar(pedidoId, (item, reserva) -> {
            item.liberarReserva(reserva.getQuantidade());
            reserva.cancelar();
            return MovimentacaoEstoque.Tipo.LIBERACAO_RESERVA;
        });
    }

    private void aplicar(UUID pedidoId, BiFunction<ItemEstoque, Reserva, MovimentacaoEstoque.Tipo> acao) {
        List<Reserva> ativas = reservas.findByPedidoIdAndStatus(pedidoId, Reserva.Status.ATIVA);
        if (ativas.isEmpty()) {
            return;
        }
        Map<UUID, ItemEstoque> estoque = itensPorInsumo(
                ativas.stream().map(Reserva::getInsumoId).collect(Collectors.toSet()));
        for (Reserva reserva : ativas) {
            MovimentacaoEstoque.Tipo tipo = acao.apply(estoque.get(reserva.getInsumoId()), reserva);
            movimentacoes.save(new MovimentacaoEstoque(reserva.getInsumoId(), tipo, reserva.getQuantidade(),
                    pedidoId, null));
        }
    }

    private Map<UUID, ItemEstoque> itensPorInsumo(Collection<UUID> insumoIds) {
        return itens.findByInsumoIdIn(insumoIds).stream()
                .collect(Collectors.toMap(ItemEstoque::getInsumoId, Function.identity()));
    }

    private void solicitarCompra(UUID pedidoId, List<InsumoFaltante> faltantes) {
        for (InsumoFaltante f : faltantes) {
            if (!solicitacoes.existsByPedidoIdAndInsumoIdAndStatus(pedidoId, f.insumoId(),
                    SolicitacaoCompra.Status.PENDENTE)) {
                solicitacoes.save(new SolicitacaoCompra(f.insumoId(), f.nomeInsumo(), f.faltante(), pedidoId));
            }
        }
    }
}
