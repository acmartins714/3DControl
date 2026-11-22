package br.edu.uniesp.threedcontrol.estoque.estoque;

import br.edu.uniesp.threedcontrol.estoque.shared.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemEstoqueTest {

    private static ItemEstoque item(String entrada, String minimo) {
        ItemEstoque i = new ItemEstoque(UUID.randomUUID(), "Filamento PLA", "KG", new BigDecimal(minimo));
        i.registrarEntrada(new BigDecimal(entrada));
        return i;
    }

    @Test
    void reservaReduzSaldoLivreSemBaixarFisico() {
        ItemEstoque i = item("10", "2");
        i.reservar(new BigDecimal("4"));
        assertThat(i.saldoLivre()).isEqualByComparingTo("6");
        assertThat(i.getQuantidadeDisponivel()).isEqualByComparingTo("10");
    }

    @Test
    void naoReservaAlemDoSaldoLivre() {
        ItemEstoque i = item("3", "0");
        assertThatThrownBy(() -> i.reservar(new BigDecimal("3.5"))).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void consumoBaixaReservaEQuantidadeFisica() {
        ItemEstoque i = item("10", "2");
        i.reservar(new BigDecimal("4"));
        i.consumirReserva(new BigDecimal("4"));
        assertThat(i.getQuantidadeDisponivel()).isEqualByComparingTo("6");
        assertThat(i.getQuantidadeReservada()).isEqualByComparingTo("0");
    }

    @Test
    void sinalizaQuandoSaldoLivreFicaAbaixoDoMinimo() {
        ItemEstoque i = item("5", "2");
        assertThat(i.abaixoDoMinimo()).isFalse();
        i.reservar(new BigDecimal("4"));
        assertThat(i.abaixoDoMinimo()).isTrue();
    }
}
