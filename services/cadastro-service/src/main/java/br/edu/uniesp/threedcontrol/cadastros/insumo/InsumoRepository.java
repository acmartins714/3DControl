package br.edu.uniesp.threedcontrol.cadastros.insumo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InsumoRepository extends JpaRepository<Insumo, UUID> {

    List<Insumo> findAllByOrderByNome();
}
