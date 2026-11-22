package br.edu.uniesp.threedcontrol.estoque.solicitacaocompra;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SolicitacaoCompraRepository extends JpaRepository<SolicitacaoCompra, UUID> {

    List<SolicitacaoCompra> findByStatusOrderBySolicitadaEm(SolicitacaoCompra.Status status);

    List<SolicitacaoCompra> findAllByOrderBySolicitadaEmDesc();

    boolean existsByPedidoIdAndInsumoIdAndStatus(UUID pedidoId, UUID insumoId, SolicitacaoCompra.Status status);
}
