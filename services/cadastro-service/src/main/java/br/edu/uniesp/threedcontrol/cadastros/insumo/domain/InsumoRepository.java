package br.edu.uniesp.threedcontrol.cadastros.insumo.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InsumoRepository {
    Insumo salvar(Insumo insumo);
    Optional<Insumo> buscarPorId(UUID id);
    List<Insumo> buscarPorIds(Collection<UUID> ids);
    List<Insumo> listar();
}
