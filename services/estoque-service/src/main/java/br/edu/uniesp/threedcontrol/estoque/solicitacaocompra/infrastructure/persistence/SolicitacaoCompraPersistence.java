package br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.infrastructure.persistence;

import br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.domain.SolicitacaoCompra;
import br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.domain.SolicitacaoCompraRepository;
import jakarta.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "solicitacao_compra")
class SolicitacaoCompraJpa {
    @Id
    UUID id;
    @Column(name = "insumo_id")
    UUID insumoId;
    @Column(name = "nome_insumo")
    String nomeInsumo;
    BigDecimal quantidade;
    @Column(name = "pedido_id")
    UUID pedidoId;
    @Enumerated(EnumType.STRING)
    SolicitacaoCompra.Status status;
    @Column(name = "solicitada_em")
    LocalDateTime solicitadaEm;
    @Column(name = "atualizada_em")
    LocalDateTime atualizadaEm;

    protected SolicitacaoCompraJpa() {
    }

    static SolicitacaoCompraJpa from(SolicitacaoCompra s) {
        SolicitacaoCompraJpa e = new SolicitacaoCompraJpa();
        e.id = s.getId();
        e.insumoId = s.getInsumoId();
        e.nomeInsumo = s.getNomeInsumo();
        e.quantidade = s.getQuantidade();
        e.pedidoId = s.getPedidoId();
        e.status = s.getStatus();
        e.solicitadaEm = s.getSolicitadaEm();
        e.atualizadaEm = s.getAtualizadaEm();
        return e;
    }

    SolicitacaoCompra toDomain() {
        return SolicitacaoCompra.restaurar(id, insumoId, nomeInsumo, quantidade, pedidoId, status, solicitadaEm, atualizadaEm);
    }
}

interface SolicitacaoCompraJpaRepository extends JpaRepository<SolicitacaoCompraJpa, UUID> {
    List<SolicitacaoCompraJpa> findByStatusOrderBySolicitadaEm(SolicitacaoCompra.Status status);
    List<SolicitacaoCompraJpa> findAllByOrderBySolicitadaEmDesc();
    boolean existsByPedidoIdAndInsumoIdAndStatus(UUID pedidoId, UUID insumoId, SolicitacaoCompra.Status status);
}

@Component
class SolicitacaoCompraRepositoryAdapter implements SolicitacaoCompraRepository {

    private final SolicitacaoCompraJpaRepository jpa;

    SolicitacaoCompraRepositoryAdapter(SolicitacaoCompraJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public SolicitacaoCompra salvar(SolicitacaoCompra solicitacao) {
        return jpa.save(SolicitacaoCompraJpa.from(solicitacao)).toDomain();
    }

    @Override
    public Optional<SolicitacaoCompra> buscarPorId(UUID id) {
        return jpa.findById(id).map(SolicitacaoCompraJpa::toDomain);
    }

    @Override
    public List<SolicitacaoCompra> listar(SolicitacaoCompra.Status status) {
        var lista = status == null ? jpa.findAllByOrderBySolicitadaEmDesc() : jpa.findByStatusOrderBySolicitadaEm(status);
        return lista.stream().map(SolicitacaoCompraJpa::toDomain).toList();
    }

    @Override
    public boolean existePendente(UUID pedidoId, UUID insumoId) {
        return jpa.existsByPedidoIdAndInsumoIdAndStatus(pedidoId, insumoId, SolicitacaoCompra.Status.PENDENTE);
    }
}
