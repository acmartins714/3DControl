package br.edu.uniesp.threedcontrol.pedidos.pedido.infrastructure.client;

import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.CadastroGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Component
class CadastroHttpGateway implements CadastroGateway {

    private static final String SERVICO = "cadastro-service";

    /** Contratos (DTOs) do cadastro-service consumidos aqui — apenas os campos necessários. */
    record ClienteDto(UUID id, String nome, boolean ativo) {
    }

    record ComposicaoDto(UUID insumoId, BigDecimal quantidade) {
    }

    record ProdutoDto(UUID id, String nome, String categoria, BigDecimal precoBase, BigDecimal custoEstimado,
                      boolean ativo, List<ComposicaoDto> composicao) {
    }

    private final RestClient rest;

    CadastroHttpGateway(RestClient.Builder builder, @Value("${integracao.cadastro-url}") String url) {
        this.rest = builder.clone().baseUrl(url).build();
    }

    @Override
    public Optional<ClienteResumo> buscarCliente(UUID clienteId) {
        return buscar(() -> rest.get().uri("/clientes/{id}", clienteId).retrieve().body(ClienteDto.class))
                .map(c -> new ClienteResumo(c.id(), c.nome(), c.ativo()));
    }

    @Override
    public Optional<ProdutoCatalogo> buscarProduto(UUID produtoId) {
        return buscar(() -> rest.get().uri("/produtos/{id}", produtoId).retrieve().body(ProdutoDto.class))
                .map(p -> new ProdutoCatalogo(p.id(), p.nome(), p.categoria(), p.precoBase(), p.custoEstimado(),
                        p.ativo(), p.composicao() == null ? List.of() : p.composicao().stream()
                        .map(c -> new InsumoNecessario(c.insumoId(), c.quantidade())).toList()));
    }

    private <T> Optional<T> buscar(Supplier<T> chamada) {
        try {
            return Optional.ofNullable(chamada.get());
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw ErrosIntegracao.traduzir(SERVICO, e);
        }
    }
}
