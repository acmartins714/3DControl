package br.edu.uniesp.threedcontrol.pedidos.pedido;

import br.edu.uniesp.threedcontrol.pedidos.shared.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PedidoTest {

    private static ItemPedido item(int quantidade, String preco) {
        return new ItemPedido(UUID.randomUUID(), "Caneca personalizada", "Canecas", quantidade,
                new BigDecimal(preco), new BigDecimal("12.00"), "Nome: Maria");
    }

    private static Pedido novoPedido(ModalidadeEntrega modalidade) {
        return new Pedido(UUID.randomUUID(), CanalAtendimento.WHATSAPP, "Ana", LocalDate.now().plusDays(5),
                modalidade, CondicaoPagamento.SINAL_E_RESTANTE_NA_ENTREGA, null, List.of(item(2, "35.00")));
    }

    @Test
    void registraPedidoEmTriagemComCodigoEHistorico() {
        Pedido p = novoPedido(ModalidadeEntrega.RETIRADA);

        assertThat(p.getStatus()).isEqualTo(StatusPedido.EM_TRIAGEM);
        assertThat(p.getCodigo()).startsWith("3DC-");
        assertThat(p.getHistorico()).hasSize(1);
        assertThat(p.valorTotal()).isEqualByComparingTo("70.00");
        assertThat(p.custoEstimadoTotal()).isEqualByComparingTo("24.00");
    }

    @Test
    void exigeAtendenteExcetoParaPedidosDoSite() {
        assertThatThrownBy(() -> new Pedido(UUID.randomUUID(), CanalAtendimento.INSTAGRAM, " ",
                LocalDate.now().plusDays(3), ModalidadeEntrega.RETIRADA, CondicaoPagamento.A_VISTA, null,
                List.of(item(1, "10"))))
                .isInstanceOf(RegraNegocioException.class);

        Pedido site = new Pedido(UUID.randomUUID(), CanalAtendimento.SITE, null, LocalDate.now().plusDays(3),
                ModalidadeEntrega.RETIRADA, CondicaoPagamento.A_VISTA, null, List.of(item(1, "10")));
        assertThat(site.getAtendenteResponsavel()).contains("Site");
    }

    @Test
    void naoAceitaPrazoNoPassado() {
        assertThatThrownBy(() -> new Pedido(UUID.randomUUID(), CanalAtendimento.TELEFONE, "Ana",
                LocalDate.now().minusDays(1), ModalidadeEntrega.RETIRADA, CondicaoPagamento.A_VISTA, null,
                List.of(item(1, "10"))))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void permiteAlterarAntesDaProducao() {
        Pedido p = novoPedido(ModalidadeEntrega.RETIRADA);

        p.alterar(LocalDate.now().plusDays(7), ModalidadeEntrega.ENTREGA, CondicaoPagamento.A_VISTA,
                "Trocar cor para azul", List.of(item(3, "35.00")));

        assertThat(p.valorTotal()).isEqualByComparingTo("105.00");
        assertThat(p.getModalidadeEntrega()).isEqualTo(ModalidadeEntrega.ENTREGA);
        assertThat(p.getHistorico()).hasSize(2);
    }

    @Test
    void regraDeOuro_bloqueiaAlteracaoAposEntrarEmProducao() {
        Pedido p = novoPedido(ModalidadeEntrega.RETIRADA);
        p.iniciarProducao();

        assertThatThrownBy(() -> p.alterar(LocalDate.now().plusDays(7), ModalidadeEntrega.RETIRADA,
                CondicaoPagamento.A_VISTA, null, List.of(item(1, "35.00"))))
                .isInstanceOf(PedidoNaoAlteravelException.class)
                .hasMessageContaining("não pode mais ser alterado");
    }

    @Test
    void pedidoAguardandoInsumoNaoEntraEmProducao() {
        Pedido p = novoPedido(ModalidadeEntrega.RETIRADA);
        p.atualizarDisponibilidadeInsumos(false, "Falta tinta");

        assertThat(p.getStatus()).isEqualTo(StatusPedido.AGUARDANDO_INSUMO);
        assertThatThrownBy(p::iniciarProducao).isInstanceOf(RegraNegocioException.class);

        p.atualizarDisponibilidadeInsumos(true, null);
        assertThat(p.getStatus()).isEqualTo(StatusPedido.EM_TRIAGEM);
    }

    @Test
    void entregaBloqueadaSemQuitacao() {
        Pedido p = novoPedido(ModalidadeEntrega.RETIRADA);
        p.iniciarProducao();
        p.finalizarProducao();

        assertThatThrownBy(() -> p.confirmarEntrega(false, new BigDecimal("35.00")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Entrega bloqueada");

        p.confirmarEntrega(true, BigDecimal.ZERO);
        assertThat(p.getStatus()).isEqualTo(StatusPedido.ENTREGUE);
    }

    @Test
    void pedidoParaRetiradaNaoPodeSerDespachado() {
        Pedido p = novoPedido(ModalidadeEntrega.RETIRADA);
        p.iniciarProducao();
        p.finalizarProducao();

        assertThatThrownBy(p::despachar).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void naoPermiteCancelarPedidoEmProducao() {
        Pedido p = novoPedido(ModalidadeEntrega.ENTREGA);
        p.iniciarProducao();

        assertThatThrownBy(() -> p.cancelar("Cliente desistiu")).isInstanceOf(RegraNegocioException.class);
    }
}
