package br.edu.uniesp.threedcontrol.estoque.estoque.infrastructure.persistence;

import br.edu.uniesp.threedcontrol.estoque.estoque.domain.EstoqueRepositories;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.ItemEstoque;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.MovimentacaoEstoque;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.Reserva;
import br.edu.uniesp.threedcontrol.estoque.estoque.infrastructure.persistence.EstoqueJpaEntities.ItemEstoqueJpa;
import br.edu.uniesp.threedcontrol.estoque.estoque.infrastructure.persistence.EstoqueJpaEntities.MovimentacaoJpa;
import br.edu.uniesp.threedcontrol.estoque.estoque.infrastructure.persistence.EstoqueJpaEntities.ReservaJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ItemEstoqueJpaRepository extends JpaRepository<ItemEstoqueJpa, UUID> {
    Optional<ItemEstoqueJpa> findByInsumoId(UUID insumoId);
    List<ItemEstoqueJpa> findByInsumoIdIn(Collection<UUID> insumoIds);
    List<ItemEstoqueJpa> findAllByOrderByNomeInsumo();
}

interface ReservaJpaRepository extends JpaRepository<ReservaJpa, UUID> {
    List<ReservaJpa> findByPedidoIdAndStatus(UUID pedidoId, Reserva.Status status);
}

interface MovimentacaoJpaRepository extends JpaRepository<MovimentacaoJpa, UUID> {
    List<MovimentacaoJpa> findByInsumoIdOrderByDataHoraDesc(UUID insumoId);
}

@Component
class ItemEstoqueRepositoryAdapter implements EstoqueRepositories.ItemEstoqueRepository {

    private final ItemEstoqueJpaRepository jpa;

    ItemEstoqueRepositoryAdapter(ItemEstoqueJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ItemEstoque salvar(ItemEstoque item) {
        return jpa.save(ItemEstoqueJpa.from(item)).toDomain();
    }

    @Override
    public void salvarTodos(Collection<ItemEstoque> itens) {
        jpa.saveAll(itens.stream().map(ItemEstoqueJpa::from).toList());
    }

    @Override
    public Optional<ItemEstoque> buscarPorInsumoId(UUID insumoId) {
        return jpa.findByInsumoId(insumoId).map(ItemEstoqueJpa::toDomain);
    }

    @Override
    public List<ItemEstoque> buscarPorInsumoIds(Collection<UUID> insumoIds) {
        return jpa.findByInsumoIdIn(insumoIds).stream().map(ItemEstoqueJpa::toDomain).toList();
    }

    @Override
    public List<ItemEstoque> listar() {
        return jpa.findAllByOrderByNomeInsumo().stream().map(ItemEstoqueJpa::toDomain).toList();
    }
}

@Component
class ReservaRepositoryAdapter implements EstoqueRepositories.ReservaRepository {

    private final ReservaJpaRepository jpa;

    ReservaRepositoryAdapter(ReservaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void salvarTodas(Collection<Reserva> reservas) {
        jpa.saveAll(reservas.stream().map(ReservaJpa::from).toList());
    }

    @Override
    public List<Reserva> buscarAtivasPorPedido(UUID pedidoId) {
        return jpa.findByPedidoIdAndStatus(pedidoId, Reserva.Status.ATIVA).stream().map(ReservaJpa::toDomain).toList();
    }
}

@Component
class MovimentacaoRepositoryAdapter implements EstoqueRepositories.MovimentacaoRepository {

    private final MovimentacaoJpaRepository jpa;

    MovimentacaoRepositoryAdapter(MovimentacaoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void registrar(MovimentacaoEstoque movimentacao) {
        jpa.save(MovimentacaoJpa.from(movimentacao));
    }

    @Override
    public List<MovimentacaoEstoque> listarPorInsumo(UUID insumoId) {
        return jpa.findByInsumoIdOrderByDataHoraDesc(insumoId).stream().map(MovimentacaoJpa::toDomain).toList();
    }
}
