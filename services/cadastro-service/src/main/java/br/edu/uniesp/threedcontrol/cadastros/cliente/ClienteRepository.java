package br.edu.uniesp.threedcontrol.cadastros.cliente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    List<Cliente> findByNomeContainingIgnoreCaseOrderByNome(String nome);

    List<Cliente> findAllByOrderByNome();

    boolean existsByDocumento(String documento);

    boolean existsByDocumentoAndIdNot(String documento, UUID id);
}
