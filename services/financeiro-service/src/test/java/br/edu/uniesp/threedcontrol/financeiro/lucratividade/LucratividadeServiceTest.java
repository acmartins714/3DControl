package br.edu.uniesp.threedcontrol.financeiro.lucratividade;

import br.edu.uniesp.threedcontrol.financeiro.contareceber.ContaReceber;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.ItemConta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LucratividadeServiceTest {

    private static ContaReceber conta(String categoria, String valor, String custo) {
        return new ContaReceber(UUID.randomUUID(), UUID.randomUUID(),
                List.of(new ItemConta(categoria, new BigDecimal(valor), new BigDecimal(custo))));
    }

    @Test
    void ignoraContasCanceladasEOrdenaPorMargem() {
        ContaReceber canecas = conta("Canecas", "100.00", "40.00");
        ContaReceber topos = conta("Topos de bolo", "200.00", "40.00");
        ContaReceber cancelada = conta("Canecas", "999.00", "1.00");
        cancelada.cancelar();

        var resultado = LucratividadeService.calcular(List.of(canecas, topos, cancelada));

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).categoria()).isEqualTo("Topos de bolo");
        assertThat(resultado.get(0).margemPercentual()).isEqualByComparingTo("80.00");
        assertThat(resultado.get(1).receita()).isEqualByComparingTo("100.00");
    }
}
