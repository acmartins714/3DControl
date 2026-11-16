package br.edu.uniesp.threedcontrol.financeiro.contareceber.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContaReceberRepository {
    ContaReceber salvar(ContaReceber conta);
    Optional<ContaReceber> buscarPorPedidoId(UUID pedidoId);
    List<ContaReceber> listar(StatusConta status);
    List<ContaReceber> listarCriadasEntre(LocalDateTime inicio, LocalDateTime fim);
}
