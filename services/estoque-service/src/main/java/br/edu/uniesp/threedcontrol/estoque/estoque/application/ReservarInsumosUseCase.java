package br.edu.uniesp.threedcontrol.estoque.estoque.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.EstoqueRepositories.ItemEstoqueRepository;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.EstoqueRepositories.MovimentacaoRepository;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.EstoqueRepositories.ReservaRepository;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.ItemEstoque;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.MovimentacaoEstoque;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.NecessidadeInsumo;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.Reserva;
import br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.domain.SolicitacaoCompra;
import br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.domain.SolicitacaoCompraRepository;

import java.math.BigDecimal;
import java.util.*;
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
public class ReservarInsumosUseCase {

    private final ItemEstoqueRepository itens;
    private final ReservaRepository reservas;
    private final MovimentacaoRepository movimentacoes;
    private final SolicitacaoCompraRepository solicitacoes;

    public ReservarInsumosUseCase(ItemEstoqueRepository itens, ReservaRepository reservas,
                                  MovimentacaoRepository movimentacoes, SolicitacaoCompraRepository solicitacoes) {
        this.itens = itens;
        this.reservas = reservas;
        this.movimentacoes = movimentacoes;
        this.solicitacoes = solicitacoes;
    }

    public ResultadoReserva reservar(UUID pedidoId, List<NecessidadeInsumo> necessidades) {
        liberar(pedidoId);

        Map<UUID, BigDecimal> totalPorInsumo = necessidades.stream()
                .collect(Collectors.toMap(NecessidadeInsumo::insumoId, NecessidadeInsumo::quantidade,
                        BigDecimal::add, LinkedHashMap::new));
        Map<UUID, ItemEstoque> estoque = itens.buscarPorInsumoIds(totalPorInsumo.keySet()).stream()
                .collect(Collectors.toMap(ItemEstoque::getInsumoId, Function.identity()));

        List<ResultadoReserva.InsumoFaltante> faltantes = new ArrayList<>();
        totalPorInsumo.forEach((insumoId, necessario) -> {
            ItemEstoque item = estoque.get(insumoId);
            BigDecimal livre = item == null ? BigDecimal.ZERO : item.saldoLivre().max(BigDecimal.ZERO);
            if (livre.compareTo(necessario) < 0) {
                String nome = item == null ? "Insumo sem controle de estoque" : item.getNomeInsumo();
                faltantes.add(new ResultadoReserva.InsumoFaltante(insumoId, nome, necessario, livre,
                        necessario.subtract(livre)));
            }
        });

        if (!faltantes.isEmpty()) {
            solicitarCompra(pedidoId, faltantes);
            return new ResultadoReserva(pedidoId, false, faltantes);
        }

        List<Reserva> novas = new ArrayList<>();
        totalPorInsumo.forEach((insumoId, quantidade) -> {
            estoque.get(insumoId).reservar(quantidade);
            novas.add(Reserva.nova(pedidoId, insumoId, quantidade));
            movimentacoes.registrar(MovimentacaoEstoque.de(insumoId, MovimentacaoEstoque.Tipo.RESERVA,
                    quantidade, pedidoId, null));
        });
        itens.salvarTodos(estoque.values());
        reservas.salvarTodas(novas);
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

    private void aplicar(UUID pedidoId, java.util.function.BiFunction<ItemEstoque, Reserva, MovimentacaoEstoque.Tipo> acao) {
        List<Reserva> ativas = reservas.buscarAtivasPorPedido(pedidoId);
        if (ativas.isEmpty()) {
            return;
        }
        Map<UUID, ItemEstoque> estoque = itens.buscarPorInsumoIds(
                        ativas.stream().map(Reserva::getInsumoId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(ItemEstoque::getInsumoId, Function.identity()));
        for (Reserva reserva : ativas) {
            ItemEstoque item = estoque.get(reserva.getInsumoId());
            MovimentacaoEstoque.Tipo tipo = acao.apply(item, reserva);
            movimentacoes.registrar(MovimentacaoEstoque.de(reserva.getInsumoId(), tipo, reserva.getQuantidade(),
                    pedidoId, null));
        }
        itens.salvarTodos(estoque.values());
        reservas.salvarTodas(ativas);
    }

    private void solicitarCompra(UUID pedidoId, List<ResultadoReserva.InsumoFaltante> faltantes) {
        for (ResultadoReserva.InsumoFaltante f : faltantes) {
            if (!solicitacoes.existePendente(pedidoId, f.insumoId())) {
                solicitacoes.salvar(SolicitacaoCompra.nova(f.insumoId(), f.nomeInsumo(), f.faltante(), pedidoId));
            }
        }
    }
}
