package br.edu.uniesp.threedcontrol.pedidos.pedido.application.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída para o cadastro-service (clientes e catálogo de produtos). */
public interface CadastroGateway {

    record ClienteResumo(UUID id, String nome, boolean ativo) {
    }

    record InsumoNecessario(UUID insumoId, BigDecimal quantidade) {
    }

    record ProdutoCatalogo(UUID id, String nome, String categoria, BigDecimal precoBase, BigDecimal custoEstimado,
                           boolean ativo, List<InsumoNecessario> composicao) {
    }

    Optional<ClienteResumo> buscarCliente(UUID clienteId);

    Optional<ProdutoCatalogo> buscarProduto(UUID produtoId);
}
