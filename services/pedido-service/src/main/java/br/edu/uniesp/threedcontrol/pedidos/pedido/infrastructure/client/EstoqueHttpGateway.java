package br.edu.uniesp.threedcontrol.pedidos.pedido.infrastructure.client;

import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.CadastroGateway.InsumoNecessario;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.EstoqueGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
class EstoqueHttpGateway implements EstoqueGateway {

    private static final String SERVICO = "estoque-service";

    record ReservaRequest(UUID pedidoId, List<InsumoNecessario> itens) {
    }

    record FaltanteDto(UUID insumoId, String nomeInsumo, BigDecimal faltante) {
    }

    record ResultadoDto(UUID pedidoId, boolean reservado, List<FaltanteDto> faltantes) {
    }

    private final RestClient rest;

    EstoqueHttpGateway(RestClient.Builder builder, @Value("${integracao.estoque-url}") String url) {
        this.rest = builder.clone().baseUrl(url).build();
    }

    @Override
    public ResultadoReserva reservar(UUID pedidoId, List<InsumoNecessario> necessidades) {
        if (necessidades.isEmpty()) {
            return new ResultadoReserva(true, List.of());
        }
        try {
            ResultadoDto r = rest.post().uri("/estoque/reservas")
                    .body(new ReservaRequest(pedidoId, necessidades))
                    .retrieve().body(ResultadoDto.class);
            if (r == null) {
                return new ResultadoReserva(false, List.of());
            }
            List<String> faltantes = r.faltantes() == null ? List.of() : r.faltantes().stream()
                    .map(f -> f.nomeInsumo() + " (faltam " + f.faltante().stripTrailingZeros().toPlainString() + ")")
                    .toList();
            return new ResultadoReserva(r.reservado(), faltantes);
        } catch (RestClientException e) {
            throw ErrosIntegracao.traduzir(SERVICO, e);
        }
    }

    @Override
    public void consumirReservas(UUID pedidoId) {
        try {
            rest.post().uri("/estoque/reservas/{id}/consumo", pedidoId).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw ErrosIntegracao.traduzir(SERVICO, e);
        }
    }

    @Override
    public void liberarReservas(UUID pedidoId) {
        try {
            rest.delete().uri("/estoque/reservas/{id}", pedidoId).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw ErrosIntegracao.traduzir(SERVICO, e);
        }
    }
}
