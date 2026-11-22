package br.edu.uniesp.threedcontrol.pedidos.pedido.integracao;

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

/** Cliente HTTP do cadastro-service: consulta clientes e o catálogo de produtos. */
@Component
public class CadastroClient {

    private static final String SERVICO = "cadastro-service";

    /** Apenas os campos do cadastro-service que o pedido usa. */
    public record Cliente(UUID id, String nome, boolean ativo) {
    }

    public record Composicao(UUID insumoId, BigDecimal quantidade) {
    }

    public record Produto(UUID id, String nome, String categoria, BigDecimal precoBase, BigDecimal custoEstimado,
                          boolean ativo, List<Composicao> composicao) {

        public List<Composicao> composicao() {
            return composicao == null ? List.of() : composicao;
        }
    }

    private final RestClient rest;

    public CadastroClient(RestClient.Builder builder, @Value("${integracao.cadastro-url}") String url) {
        this.rest = builder.clone().baseUrl(url).build();
    }

    public Optional<Cliente> buscarCliente(UUID clienteId) {
        return buscar(() -> rest.get().uri("/clientes/{id}", clienteId).retrieve().body(Cliente.class));
    }

    public Optional<Produto> buscarProduto(UUID produtoId) {
        return buscar(() -> rest.get().uri("/produtos/{id}", produtoId).retrieve().body(Produto.class));
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
