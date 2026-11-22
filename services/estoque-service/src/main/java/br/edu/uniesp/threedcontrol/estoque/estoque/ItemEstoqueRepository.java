package br.edu.uniesp.threedcontrol.estoque.estoque;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemEstoqueRepository extends JpaRepository<ItemEstoque, UUID> {

    Optional<ItemEstoque> findByInsumoId(UUID insumoId);

    List<ItemEstoque> findByInsumoIdIn(Collection<UUID> insumoIds);

    List<ItemEstoque> findAllByOrderByNomeInsumo();
}
