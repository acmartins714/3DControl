package br.edu.uniesp.threedcontrol.estoque.solicitacaocompra;

import br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.SolicitacaoCompraDtos.RecebimentoRequest;
import br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.SolicitacaoCompraDtos.SolicitacaoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/solicitacoes-compra")
public class SolicitacaoCompraController {

    private final SolicitacaoCompraService service;

    public SolicitacaoCompraController(SolicitacaoCompraService service) {
        this.service = service;
    }

    @GetMapping
    public List<SolicitacaoResponse> listar(@RequestParam(required = false) SolicitacaoCompra.Status status,
                                            @RequestParam(required = false) UUID pedidoId) {
        return service.listar(status, pedidoId);
    }

    @PostMapping("/{id}/recebimento")
    public SolicitacaoResponse receber(@PathVariable UUID id, @RequestBody(required = false) RecebimentoRequest request) {
        return service.registrarRecebimento(id, request == null ? null : request.quantidadeRecebida());
    }

    @PostMapping("/{id}/cancelamento")
    public SolicitacaoResponse cancelar(@PathVariable UUID id) {
        return service.cancelar(id);
    }
}
