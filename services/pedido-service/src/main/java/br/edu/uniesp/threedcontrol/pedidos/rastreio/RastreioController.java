package br.edu.uniesp.threedcontrol.pedidos.rastreio;

import br.edu.uniesp.threedcontrol.pedidos.rastreio.RastreioService.Rastreio;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rastreio")
public class RastreioController {

    private final RastreioService service;

    public RastreioController(RastreioService service) {
        this.service = service;
    }

    @GetMapping("/{codigo}")
    public Rastreio rastrear(@PathVariable String codigo) {
        return service.rastrear(codigo);
    }
}
