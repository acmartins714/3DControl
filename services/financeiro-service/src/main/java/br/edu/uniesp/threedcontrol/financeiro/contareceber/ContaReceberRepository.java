package br.edu.uniesp.threedcontrol.financeiro.contareceber;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContaReceberRepository extends JpaRepository<ContaReceber, UUID> {

    Optional<ContaReceber> findByPedidoId(UUID pedidoId);

    List<ContaReceber> findAllByOrderByCriadaEmDesc();

    List<ContaReceber> findByStatusOrderByCriadaEmDesc(StatusConta status);

    List<ContaReceber> findByCriadaEmBetween(LocalDateTime inicio, LocalDateTime fim);
}
