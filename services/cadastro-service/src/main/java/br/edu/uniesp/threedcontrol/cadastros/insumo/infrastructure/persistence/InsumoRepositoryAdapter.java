package br.edu.uniesp.threedcontrol.cadastros.insumo.infrastructure.persistence;

import br.edu.uniesp.threedcontrol.cadastros.insumo.domain.Insumo;
import br.edu.uniesp.threedcontrol.cadastros.insumo.domain.InsumoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface InsumoJpaRepository extends JpaRepository<InsumoJpaEntity, UUID> {
}

@Component
class InsumoRepositoryAdapter implements InsumoRepository {

    private final InsumoJpaRepository jpa;

    InsumoRepositoryAdapter(InsumoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Insumo salvar(Insumo insumo) {
        return jpa.save(InsumoJpaEntity.from(insumo)).toDomain();
    }

    @Override
    public Optional<Insumo> buscarPorId(UUID id) {
        return jpa.findById(id).map(InsumoJpaEntity::toDomain);
    }

    @Override
    public List<Insumo> buscarPorIds(Collection<UUID> ids) {
        return jpa.findAllById(ids).stream().map(InsumoJpaEntity::toDomain).toList();
    }

    @Override
    public List<Insumo> listar() {
        return jpa.findAll(Sort.by("nome")).stream().map(InsumoJpaEntity::toDomain).toList();
    }
}
