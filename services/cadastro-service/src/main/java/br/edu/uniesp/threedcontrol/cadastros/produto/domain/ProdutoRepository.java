package br.edu.uniesp.threedcontrol.cadastros.produto.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProdutoRepository {
    Produto salvar(Produto produto);
    Optional<Produto> buscarPorId(UUID id);
    List<Produto> listar(String categoria);
}
