package br.edu.uniesp.threedcontrol.producao.ordemproducao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdemProducaoRepository extends JpaRepository<OrdemProducao, UUID> {

    /** Ordem mais recente do pedido (um pedido cancelado e refeito pode ter mais de uma). */
    Optional<OrdemProducao> findFirstByPedidoIdOrderByCriadaEmDesc(UUID pedidoId);

    List<OrdemProducao> findByStatusIn(Collection<StatusOrdem> status);
}
