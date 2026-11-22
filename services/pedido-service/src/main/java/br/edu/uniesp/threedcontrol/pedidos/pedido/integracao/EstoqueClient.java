package br.edu.uniesp.threedcontrol.pedidos.pedido.integracao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Cliente HTTP do estoque-service: reserva, consumo e liberação de insumos. */
@Component
public class EstoqueClient {

    private static final String SERVICO = "estoque-service";

    /** Quantidade total de um insumo exigida pelo pedido (ficha técnica × quantidade). */
    public record InsumoNecessario(UUID insumoId, BigDecimal quantidade) {
    }

    /** {@code insumosFaltantes} já vem formatado para o histórico do pedido. */
    public record ResultadoReserva(boolean reservado, List<String> insumosFaltantes) {
    }

    record ReservaRequest(UUID pedidoId, List<InsumoNecessario> itens) {
    }

    record Faltante(UUID insumoId, String nomeInsumo, BigDecimal faltante) {
    }

    record ResultadoResponse(UUID pedidoId, boolean reservado, List<Faltante> faltantes) {
    }

    private final RestClient rest;

    public EstoqueClient(RestClient.Builder builder, @Value("${integracao.estoque-url}") String url) {
        this.rest = builder.clone().baseUrl(url).build();
    }

    /** Reserva (ou re-reserva, de forma idempotente) os insumos do pedido. */
    public ResultadoReserva reservar(UUID pedidoId, List<InsumoNecessario> necessidades) {
        if (necessidades.isEmpty()) {
            return new ResultadoReserva(true, List.of());
        }
        try {
            ResultadoResponse r = rest.post().uri("/estoque/reservas")
                    .body(new ReservaRequest(pedidoId, necessidades))
                    .retrieve().body(ResultadoResponse.class);
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

    public void consumirReservas(UUID pedidoId) {
        try {
            rest.post().uri("/estoque/reservas/{id}/consumo", pedidoId).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw ErrosIntegracao.traduzir(SERVICO, e);
        }
    }

    public void liberarReservas(UUID pedidoId) {
        try {
            rest.delete().uri("/estoque/reservas/{id}", pedidoId).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw ErrosIntegracao.traduzir(SERVICO, e);
        }
    }
}
