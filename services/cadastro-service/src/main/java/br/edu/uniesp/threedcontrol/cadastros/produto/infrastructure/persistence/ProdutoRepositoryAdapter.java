package br.edu.uniesp.threedcontrol.cadastros.produto.infrastructure.persistence;

import br.edu.uniesp.threedcontrol.cadastros.produto.domain.Produto;
import br.edu.uniesp.threedcontrol.cadastros.produto.domain.ProdutoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ProdutoJpaRepository extends JpaRepository<ProdutoJpaEntity, UUID> {
    List<ProdutoJpaEntity> findByCategoriaIgnoreCaseOrderByNome(String categoria);
}

@Component
class ProdutoRepositoryAdapter implements ProdutoRepository {

    private final ProdutoJpaRepository jpa;

    ProdutoRepositoryAdapter(ProdutoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Produto salvar(Produto produto) {
        return jpa.save(ProdutoJpaEntity.from(produto)).toDomain();
    }

    @Override
    public Optional<Produto> buscarPorId(UUID id) {
        return jpa.findById(id).map(ProdutoJpaEntity::toDomain);
    }

    @Override
    public List<Produto> listar(String categoria) {
        var lista = (categoria == null || categoria.isBlank())
                ? jpa.findAll(Sort.by("nome"))
                : jpa.findByCategoriaIgnoreCaseOrderByNome(categoria);
        return lista.stream().map(ProdutoJpaEntity::toDomain).toList();
    }
}
