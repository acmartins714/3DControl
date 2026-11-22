package br.edu.uniesp.threedcontrol.cadastros.produto;

import br.edu.uniesp.threedcontrol.cadastros.shared.RegraNegocioException;
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
        Produto p = new Produto("Chaveiro 3D", null, "Chaveiros", null, 2, new BigDecimal("15.00"),
                List.of(new ItemComposicao(filamento, new BigDecimal("0.05")),
                        new ItemComposicao(tinta, new BigDecimal("2"))));

        BigDecimal custo = p.calcularCustoInsumos(Map.of(filamento, new BigDecimal("120.00"), tinta, new BigDecimal("0.50")));

        assertThat(custo).isEqualByComparingTo("7.00");
    }

    @Test
    void naoPermiteInsumoRepetidoNaComposicao() {
        UUID insumo = UUID.randomUUID();
        assertThatThrownBy(() -> new Produto("Caneca", null, "Canecas", null, 1, BigDecimal.TEN,
                List.of(new ItemComposicao(insumo, BigDecimal.ONE), new ItemComposicao(insumo, BigDecimal.TEN))))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void naoPermiteQuantidadeZeroNaComposicao() {
        assertThatThrownBy(() -> new ItemComposicao(UUID.randomUUID(), BigDecimal.ZERO))
                .isInstanceOf(RegraNegocioException.class);
    }
}
