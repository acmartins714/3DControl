package br.edu.uniesp.threedcontrol.cadastros.produto.domain;

import br.edu.uniesp.threedcontrol.cadastros.shared.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProdutoTest {

    @Test
    void calculaCustoDosInsumosPelaFichaTecnica() {
        UUID filamento = UUID.randomUUID();
        UUID tinta = UUID.randomUUID();
        Produto p = Produto.novo("Chaveiro 3D", null, "Chaveiros", null, 2, new BigDecimal("15.00"),
                List.of(new ComposicaoProduto(filamento, new BigDecimal("0.05")),
                        new ComposicaoProduto(tinta, new BigDecimal("2"))));

        BigDecimal custo = p.calcularCustoInsumos(Map.of(filamento, new BigDecimal("120.00"), tinta, new BigDecimal("0.50")));

        assertThat(custo).isEqualByComparingTo("7.00");
    }

    @Test
    void naoPermiteInsumoRepetidoNaComposicao() {
        UUID insumo = UUID.randomUUID();

        assertThatThrownBy(() -> Produto.novo("Caneca", null, "Canecas", null, 1, BigDecimal.TEN,
                List.of(new ComposicaoProduto(insumo, BigDecimal.ONE), new ComposicaoProduto(insumo, BigDecimal.TEN))))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void naoPermiteQuantidadeZeroNaComposicao() {
        assertThatThrownBy(() -> new ComposicaoProduto(UUID.randomUUID(), BigDecimal.ZERO))
                .isInstanceOf(DomainException.class);
    }
}
