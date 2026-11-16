package br.edu.uniesp.threedcontrol.pedidos.rastreio.infrastructure;

import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.PedidoRepository;
import br.edu.uniesp.threedcontrol.pedidos.rastreio.application.RastrearPedidoUseCase;
import br.edu.uniesp.threedcontrol.pedidos.rastreio.application.RastrearPedidoUseCase.Rastreio;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rastreio")
class RastreioController {

    private final RastrearPedidoUseCase useCase;

    RastreioController(RastrearPedidoUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/{codigo}")
    Rastreio rastrear(@PathVariable String codigo) {
        return useCase.rastrear(codigo);
    }
}

@Configuration
class RastreioConfig {

    @Bean
    RastrearPedidoUseCase rastrearPedidoUseCase(PedidoRepository pedidos) {
        return new RastrearPedidoUseCase(pedidos);
    }
}
