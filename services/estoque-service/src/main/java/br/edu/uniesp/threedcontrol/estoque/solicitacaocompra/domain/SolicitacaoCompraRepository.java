package br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoCompraRepository {
    SolicitacaoCompra salvar(SolicitacaoCompra solicitacao);
    Optional<SolicitacaoCompra> buscarPorId(UUID id);
    List<SolicitacaoCompra> listar(SolicitacaoCompra.Status status);
    boolean existePendente(UUID pedidoId, UUID insumoId);
}
