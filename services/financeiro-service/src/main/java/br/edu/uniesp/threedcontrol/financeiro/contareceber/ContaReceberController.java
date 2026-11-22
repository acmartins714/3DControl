package br.edu.uniesp.threedcontrol.financeiro.contareceber;

import br.edu.uniesp.threedcontrol.financeiro.contareceber.ContaReceberDtos.AberturaRequest;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.ContaReceberDtos.ContaResponse;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.ContaReceberDtos.ItensRequest;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.ContaReceberDtos.PagamentoRequest;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.ContaReceberDtos.SituacaoFinanceira;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/financeiro/contas")
public class ContaReceberController {

    private final ContaReceberService service;

    public ContaReceberController(ContaReceberService service) {
        this.service = service;
    }

    /** Usado pelo pedido-service ao registrar um pedido. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContaResponse abrir(@Valid @RequestBody AberturaRequest request) {
        return service.abrir(request);
    }

    @GetMapping
    public List<ContaResponse> listar(@RequestParam(required = false) StatusConta status) {
        return service.listar(status);
    }

    @GetMapping("/pedido/{pedidoId}")
    public ContaResponse buscar(@PathVariable UUID pedidoId) {
        return service.buscar(pedidoId);
    }

    /** Usado pelo pedido-service quando o pedido é alterado. */
    @PutMapping("/pedido/{pedidoId}/itens")
    public ContaResponse atualizarItens(@PathVariable UUID pedidoId, @Valid @RequestBody ItensRequest request) {
        return service.atualizarItens(pedidoId, request.itens());
    }

    @PostMapping("/pedido/{pedidoId}/pagamentos")
    public ContaResponse registrarPagamento(@PathVariable UUID pedidoId, @Valid @RequestBody PagamentoRequest request) {
        return service.registrarPagamento(pedidoId, request);
    }

    /** Usado pelo pedido-service para autorizar a entrega. */
    @GetMapping("/pedido/{pedidoId}/situacao")
    public SituacaoFinanceira situacao(@PathVariable UUID pedidoId) {
        return service.situacao(pedidoId);
    }

    /** Usado pelo pedido-service quando o pedido é cancelado. */
    @DeleteMapping("/pedido/{pedidoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable UUID pedidoId) {
        service.cancelar(pedidoId);
    }
}
