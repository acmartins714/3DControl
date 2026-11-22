package br.edu.uniesp.threedcontrol.cadastros.produto;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProdutoRepository extends JpaRepository<Produto, UUID> {

    List<Produto> findAllByOrderByNome();

    List<Produto> findByCategoriaIgnoreCaseOrderByNome(String categoria);
}
