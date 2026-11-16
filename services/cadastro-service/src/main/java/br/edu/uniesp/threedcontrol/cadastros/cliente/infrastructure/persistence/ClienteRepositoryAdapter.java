package br.edu.uniesp.threedcontrol.cadastros.cliente.infrastructure.persistence;

import br.edu.uniesp.threedcontrol.cadastros.cliente.domain.Cliente;
import br.edu.uniesp.threedcontrol.cadastros.cliente.domain.ClienteRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador que implementa a porta do domínio usando Spring Data JPA. */
@Component
class ClienteRepositoryAdapter implements ClienteRepository {

    private final ClienteJpaRepository jpa;

    ClienteRepositoryAdapter(ClienteJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Cliente salvar(Cliente cliente) {
        return jpa.save(ClienteJpaEntity.from(cliente)).toDomain();
    }

    @Override
    public Optional<Cliente> buscarPorId(UUID id) {
        return jpa.findById(id).map(ClienteJpaEntity::toDomain);
    }

    @Override
    public List<Cliente> listar(String nomeContendo) {
        var resultado = (nomeContendo == null || nomeContendo.isBlank())
                ? jpa.findAllByOrderByNome()
                : jpa.findByNomeContainingIgnoreCaseOrderByNome(nomeContendo);
        return resultado.stream().map(ClienteJpaEntity::toDomain).toList();
    }

    @Override
    public boolean existeOutroComDocumento(String documento, UUID idIgnorado) {
        return idIgnorado == null
                ? jpa.existsByDocumento(documento)
                : jpa.existsByDocumentoAndIdNot(documento, idIgnorado);
    }
}
