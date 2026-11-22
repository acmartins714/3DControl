package br.edu.uniesp.threedcontrol.cadastros.produto;

import br.edu.uniesp.threedcontrol.cadastros.shared.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Linha da ficha técnica: quantidade de um insumo necessária para fabricar UMA unidade do produto. */
@Embeddable
public class ItemComposicao {

    @Column(name = "insumo_id")
    private UUID insumoId;
    private BigDecimal quantidade;

    protected ItemComposicao() {
    }

    public ItemComposicao(UUID insumoId, BigDecimal quantidade) {
        Objects.requireNonNull(insumoId, "insumoId é obrigatório");
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new RegraNegocioException("A quantidade de cada insumo na composição deve ser maior que zero.");
        }
        this.insumoId = insumoId;
        this.quantidade = quantidade;
    }

    public UUID getInsumoId() { return insumoId; }
    public BigDecimal getQuantidade() { return quantidade; }
}
