package br.edu.uniesp.threedcontrol.estoque.estoque.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Portas de persistência do contexto de estoque. */
public final class EstoqueRepositories {

    private EstoqueRepositories() {
    }

    public interface ItemEstoqueRepository {
        ItemEstoque salvar(ItemEstoque item);
        void salvarTodos(Collection<ItemEstoque> itens);
        Optional<ItemEstoque> buscarPorInsumoId(UUID insumoId);
        List<ItemEstoque> buscarPorInsumoIds(Collection<UUID> insumoIds);
        List<ItemEstoque> listar();
    }

    public interface ReservaRepository {
        void salvarTodas(Collection<Reserva> reservas);
        List<Reserva> buscarAtivasPorPedido(UUID pedidoId);
    }

    public interface MovimentacaoRepository {
        void registrar(MovimentacaoEstoque movimentacao);
        List<MovimentacaoEstoque> listarPorInsumo(UUID insumoId);
    }
}
