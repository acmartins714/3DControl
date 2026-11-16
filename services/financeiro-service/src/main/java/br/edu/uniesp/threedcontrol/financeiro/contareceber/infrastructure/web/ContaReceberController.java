package br.edu.uniesp.threedcontrol.financeiro.contareceber.infrastructure.web;

import br.edu.uniesp.threedcontrol.financeiro.contareceber.application.GerenciarContasReceberUseCase;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.application.GerenciarContasReceberUseCase.SituacaoFinanceira;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.domain.ContaReceber;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.domain.ItemConta;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.domain.Pagamento;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.domain.StatusConta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/financeiro/contas")
class ContaReceberController {

    record ItemRequest(@NotBlank String categoria, @NotNull @DecimalMin("0.0") BigDecimal valorVenda,
                       @NotNull @DecimalMin("0.0") BigDecimal custoEstimado) {
        ItemConta toDomain() {
            return new ItemConta(categoria, valorVenda, custoEstimado);
        }
    }

    record AberturaRequest(@NotNull UUID pedidoId, @NotNull UUID clienteId, @NotEmpty List<@Valid ItemRequest> itens) {
    }

    record ItensRequest(@NotEmpty List<@Valid ItemRequest> itens) {
    }

    record PagamentoRequest(@NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal valor,
                            @NotNull Pagamento.FormaPagamento forma, String observacao) {
    }

    record ContaResponse(UUID id, UUID pedidoId, UUID clienteId, StatusConta status, BigDecimal valorTotal,
                         BigDecimal custoEstimado, BigDecimal totalPago, BigDecimal saldoRemanescente,
                         boolean liberadaParaEntrega, List<ItemConta> itens, List<Pagamento> pagamentos,
                         LocalDateTime criadaEm, LocalDateTime atualizadaEm) {
        static ContaResponse from(ContaReceber c) {
            return new ContaResponse(c.getId(), c.getPedidoId(), c.getClienteId(), c.getStatus(), c.valorTotal(),
                    c.custoTotal(), c.totalPago(), c.saldoRemanescente(), c.liberadaParaEntrega(), c.getItens(),
                    c.getPagamentos(), c.getCriadaEm(), c.getAtualizadaEm());
        }
    }

    private final GerenciarContasReceberUseCase useCase;

    ContaReceberController(GerenciarContasReceberUseCase useCase) {
        this.useCase = useCase;
    }

    /** Usado pelo pedido-service ao registrar um pedido. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ContaResponse abrir(@Valid @RequestBody AberturaRequest r) {
        return ContaResponse.from(useCase.abrir(r.pedidoId(), r.clienteId(),
                r.itens().stream().map(ItemRequest::toDomain).toList()));
    }

    @GetMapping
    List<ContaResponse> listar(@RequestParam(required = false) StatusConta status) {
        return useCase.listar(status).stream().map(ContaResponse::from).toList();
    }

    @GetMapping("/pedido/{pedidoId}")
    ContaResponse buscar(@PathVariable UUID pedidoId) {
        return ContaResponse.from(useCase.buscar(pedidoId));
    }

    /** Usado pelo pedido-service quando o pedido é alterado. */
    @PutMapping("/pedido/{pedidoId}/itens")
    ContaResponse atualizarItens(@PathVariable UUID pedidoId, @Valid @RequestBody ItensRequest r) {
        return ContaResponse.from(useCase.atualizarItens(pedidoId, r.itens().stream().map(ItemRequest::toDomain).toList()));
    }

    @PostMapping("/pedido/{pedidoId}/pagamentos")
    ContaResponse registrarPagamento(@PathVariable UUID pedidoId, @Valid @RequestBody PagamentoRequest r) {
        return ContaResponse.from(useCase.registrarPagamento(pedidoId, r.valor(), r.forma(), r.observacao()));
    }

    /** Usado pelo pedido-service para autorizar a entrega. */
    @GetMapping("/pedido/{pedidoId}/situacao")
    SituacaoFinanceira situacao(@PathVariable UUID pedidoId) {
        return useCase.situacao(pedidoId);
    }

    /** Usado pelo pedido-service quando o pedido é cancelado. */
    @DeleteMapping("/pedido/{pedidoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancelar(@PathVariable UUID pedidoId) {
        useCase.cancelar(pedidoId);
    }
}
