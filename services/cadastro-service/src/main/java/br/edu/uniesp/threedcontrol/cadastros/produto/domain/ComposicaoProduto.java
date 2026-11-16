package br.edu.uniesp.threedcontrol.cadastros.produto.domain;

import br.edu.uniesp.threedcontrol.cadastros.shared.domain.DomainException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Objeto de valor: quantidade de um insumo necessária para fabricar UMA unidade do produto. */
public record ComposicaoProduto(UUID insumoId, BigDecimal quantidade) {

    public ComposicaoProduto {
        Objects.requireNonNull(insumoId, "insumoId é obrigatório");
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new DomainException("A quantidade de cada insumo na composição deve ser maior que zero.");
        }
    }
}
