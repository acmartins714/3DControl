package br.edu.uniesp.threedcontrol.estoque.estoque.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Resultado da tentativa de reservar os insumos de um pedido. */
public record ResultadoReserva(UUID pedidoId, boolean reservado, List<InsumoFaltante> faltantes) {

    public record InsumoFaltante(UUID insumoId, String nomeInsumo, BigDecimal necessario,
                                 BigDecimal disponivel, BigDecimal faltante) {
    }
}
