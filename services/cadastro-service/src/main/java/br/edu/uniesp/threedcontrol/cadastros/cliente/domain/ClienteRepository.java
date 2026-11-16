package br.edu.uniesp.threedcontrol.cadastros.cliente.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída (persistência) definida pelo domínio e implementada na infraestrutura. */
public interface ClienteRepository {
    Cliente salvar(Cliente cliente);
    Optional<Cliente> buscarPorId(UUID id);
    List<Cliente> listar(String nomeContendo);
    boolean existeOutroComDocumento(String documento, UUID idIgnorado);
}
