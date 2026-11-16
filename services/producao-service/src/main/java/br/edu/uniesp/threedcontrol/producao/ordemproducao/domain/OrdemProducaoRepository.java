package br.edu.uniesp.threedcontrol.producao.ordemproducao.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdemProducaoRepository {
    OrdemProducao salvar(OrdemProducao ordem);
    Optional<OrdemProducao> buscarPorId(UUID id);
    Optional<OrdemProducao> buscarPorPedidoId(UUID pedidoId);
    List<OrdemProducao> listarPorStatus(Collection<StatusOrdem> status);
}
