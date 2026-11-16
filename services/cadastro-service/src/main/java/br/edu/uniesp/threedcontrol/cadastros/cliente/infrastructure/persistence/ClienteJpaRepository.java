package br.edu.uniesp.threedcontrol.cadastros.cliente.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ClienteJpaRepository extends JpaRepository<ClienteJpaEntity, UUID> {
    List<ClienteJpaEntity> findByNomeContainingIgnoreCaseOrderByNome(String nome);
    List<ClienteJpaEntity> findAllByOrderByNome();
    boolean existsByDocumentoAndIdNot(String documento, UUID id);
    boolean existsByDocumento(String documento);
}
