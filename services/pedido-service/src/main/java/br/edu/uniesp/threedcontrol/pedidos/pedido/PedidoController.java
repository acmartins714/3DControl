package br.edu.uniesp.threedcontrol.pedidos.pedido;

import br.edu.uniesp.threedcontrol.pedidos.pedido.PedidoDtos.AlteracaoRequest;
import br.edu.uniesp.threedcontrol.pedidos.pedido.PedidoDtos.CancelamentoRequest;
import br.edu.uniesp.threedcontrol.pedidos.pedido.PedidoDtos.NovoPedidoRequest;
import br.edu.uniesp.threedcontrol.pedidos.pedido.PedidoDtos.PedidoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse registrar(@Valid @RequestBody NovoPedidoRequest request) {
        return service.registrar(request);
    }

    @GetMapping
    public List<PedidoResponse> listar(@RequestParam(required = false) StatusPedido status,
                                       @RequestParam(required = false) UUID clienteId) {
        return service.listar(status, clienteId);
    }

    @GetMapping("/{id}")
    public PedidoResponse buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    /** Alteração: permitida apenas antes da entrada em produção (Regra de Ouro). */
    @PutMapping("/{id}")
    public PedidoResponse alterar(@PathVariable UUID id, @Valid @RequestBody AlteracaoRequest request) {
        return service.alterar(id, request);
    }

    @PostMapping("/{id}/reprocessamento-insumos")
    public PedidoResponse reprocessarInsumos(@PathVariable UUID id) {
        return service.reprocessarInsumos(id);
    }

    /** Integração: chamado pelo producao-service ao iniciar a ordem. */
    @PostMapping("/{id}/inicio-producao")
    public PedidoResponse iniciarProducao(@PathVariable UUID id) {
        return service.iniciarProducao(id);
    }

    /** Integração: chamado pelo producao-service ao concluir a ordem. */
    @PostMapping("/{id}/conclusao-producao")
    public PedidoResponse finalizarProducao(@PathVariable UUID id) {
        return service.finalizarProducao(id);
    }

    @PostMapping("/{id}/despacho")
    public PedidoResponse despachar(@PathVariable UUID id) {
        return service.despachar(id);
    }

    /** Validação na entrega: bloqueada enquanto houver saldo a receber. */
    @PostMapping("/{id}/entrega")
    public PedidoResponse confirmarEntrega(@PathVariable UUID id) {
        return service.confirmarEntrega(id);
    }

    @PostMapping("/{id}/cancelamento")
    public PedidoResponse cancelar(@PathVariable UUID id, @RequestBody(required = false) CancelamentoRequest request) {
        return service.cancelar(id, request == null ? null : request.motivo());
    }
}
