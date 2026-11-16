package br.edu.uniesp.threedcontrol.pedidos.pedido.infrastructure.config;

import br.edu.uniesp.threedcontrol.pedidos.pedido.application.*;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.CadastroGateway;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.EstoqueGateway;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.FinanceiroGateway;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.port.ProducaoGateway;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.PedidoRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PedidoConfig {

    @Bean
    MontadorItensPedido montadorItensPedido(CadastroGateway cadastro) {
        return new MontadorItensPedido(cadastro);
    }

    @Bean
    RegistrarPedidoUseCase registrarPedidoUseCase(PedidoRepository pedidos, CadastroGateway cadastro,
                                                  MontadorItensPedido montador, EstoqueGateway estoque,
                                                  ProducaoGateway producao, FinanceiroGateway financeiro) {
        return new RegistrarPedidoUseCase(pedidos, cadastro, montador, estoque, producao, financeiro);
    }

    @Bean
    AlterarPedidoUseCase alterarPedidoUseCase(PedidoRepository pedidos, MontadorItensPedido montador,
                                              EstoqueGateway estoque, ProducaoGateway producao,
                                              FinanceiroGateway financeiro) {
        return new AlterarPedidoUseCase(pedidos, montador, estoque, producao, financeiro);
    }

    @Bean
    MovimentarPedidoUseCase movimentarPedidoUseCase(PedidoRepository pedidos, MontadorItensPedido montador,
                                                    EstoqueGateway estoque, ProducaoGateway producao,
                                                    FinanceiroGateway financeiro) {
        return new MovimentarPedidoUseCase(pedidos, montador, estoque, producao, financeiro);
    }

    @Bean
    ConsultarPedidosUseCase consultarPedidosUseCase(PedidoRepository pedidos) {
        return new ConsultarPedidosUseCase(pedidos);
    }
}
