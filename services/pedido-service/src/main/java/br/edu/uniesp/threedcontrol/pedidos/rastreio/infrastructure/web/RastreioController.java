package br.edu.uniesp.threedcontrol.pedidos.rastreio.infrastructure.web;

import br.edu.uniesp.threedcontrol.pedidos.rastreio.application.RastrearPedidoUseCase;
import br.edu.uniesp.threedcontrol.pedidos.rastreio.application.RastrearPedidoUseCase.Rastreio;
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
