package br.edu.uniesp.threedcontrol.financeiro.contareceber;

import br.edu.uniesp.threedcontrol.financeiro.contareceber.Pagamento.FormaPagamento;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.Pagamento.TipoPagamento;
import br.edu.uniesp.threedcontrol.financeiro.shared.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContaReceberTest {

    private static ContaReceber conta(String valor, String custo) {
        return new ContaReceber(UUID.randomUUID(), UUID.randomUUID(),
                List.of(new ItemConta("Canecas", new BigDecimal(valor), new BigDecimal(custo))));
    }

    @Test
    void sinalDepoisQuitacaoLiberamEntrega() {
        ContaReceber c = conta("100.00", "40.00");

        Pagamento sinal = c.registrarPagamento(new BigDecimal("50.00"), FormaPagamento.PIX, null);
        assertThat(sinal.getTipo()).isEqualTo(TipoPagamento.SINAL);
        assertThat(c.getStatus()).isEqualTo(StatusConta.PARCIALMENTE_PAGA);
        assertThat(c.liberadaParaEntrega()).isFalse();

        Pagamento quitacao = c.registrarPagamento(new BigDecimal("50.00"), FormaPagamento.DINHEIRO, null);
        assertThat(quitacao.getTipo()).isEqualTo(TipoPagamento.QUITACAO);
        assertThat(c.saldoRemanescente()).isEqualByComparingTo("0");
        assertThat(c.liberadaParaEntrega()).isTrue();
    }

    @Test
    void naoAceitaPagamentoMaiorQueSaldo() {
        ContaReceber c = conta("100.00", "40.00");
        assertThatThrownBy(() -> c.registrarPagamento(new BigDecimal("100.01"), FormaPagamento.PIX, null))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void naoPermiteReduzirValorAbaixoDoJaPago() {
        ContaReceber c = conta("100.00", "40.00");
        c.registrarPagamento(new BigDecimal("80.00"), FormaPagamento.PIX, null);
        assertThatThrownBy(() -> c.atualizarItens(List.of(new ItemConta("Canecas", new BigDecimal("60"), BigDecimal.ONE))))
                .isInstanceOf(RegraNegocioException.class);
    }
}
