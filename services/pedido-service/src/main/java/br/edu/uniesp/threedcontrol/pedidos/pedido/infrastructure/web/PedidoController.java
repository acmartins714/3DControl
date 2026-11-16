package br.edu.uniesp.threedcontrol.pedidos.pedido.infrastructure.web;

import br.edu.uniesp.threedcontrol.pedidos.pedido.application.*;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.PedidoCommands.AlteracaoPedido;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.PedidoCommands.ItemCommand;
import br.edu.uniesp.threedcontrol.pedidos.pedido.application.PedidoCommands.NovoPedido;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.Enums.CanalAtendimento;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.Enums.CondicaoPagamento;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.Enums.ModalidadeEntrega;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.HistoricoStatus;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.ItemPedido;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.Pedido;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.StatusPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pedidos")
class PedidoController {

    record ItemRequest(@NotNull UUID produtoId, @Positive int quantidade,
                       @DecimalMin("0.0") BigDecimal precoUnitario, String personalizacao) {
        ItemCommand toCommand() {
            return new ItemCommand(produtoId, quantidade, precoUnitario, personalizacao);
        }
    }

    record NovoPedidoRequest(@NotNull UUID clienteId, @NotNull CanalAtendimento canalOrigem,
                             String atendenteResponsavel, @NotNull @FutureOrPresent LocalDate prazoEntrega,
                             @NotNull ModalidadeEntrega modalidadeEntrega, @NotNull CondicaoPagamento condicaoPagamento,
                             String observacoes, @NotEmpty List<@Valid ItemRequest> itens) {
        NovoPedido toCommand() {
            return new NovoPedido(clienteId, canalOrigem, atendenteResponsavel, prazoEntrega, modalidadeEntrega,
                    condicaoPagamento, observacoes, itens.stream().map(ItemRequest::toCommand).toList());
        }
    }

    record AlteracaoRequest(@NotNull @FutureOrPresent LocalDate prazoEntrega, @NotNull ModalidadeEntrega modalidadeEntrega,
                            @NotNull CondicaoPagamento condicaoPagamento, String observacoes,
                            @NotEmpty List<@Valid ItemRequest> itens) {
        AlteracaoPedido toCommand() {
            return new AlteracaoPedido(prazoEntrega, modalidadeEntrega, condicaoPagamento, observacoes,
                    itens.stream().map(ItemRequest::toCommand).toList());
        }
    }

    record CancelamentoRequest(String motivo) {
    }

    record PedidoResponse(UUID id, String codigo, UUID clienteId, CanalAtendimento canalOrigem,
                          String atendenteResponsavel, LocalDate prazoEntrega, ModalidadeEntrega modalidadeEntrega,
                          CondicaoPagamento condicaoPagamento, String observacoes, StatusPedido status,
                          boolean permiteAlteracao, BigDecimal valorTotal, BigDecimal custoEstimado,
                          List<ItemPedido> itens, List<HistoricoStatus> historico,
                          LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        static PedidoResponse from(Pedido p) {
            return new PedidoResponse(p.getId(), p.getCodigo(), p.getClienteId(), p.getCanalOrigem(),
                    p.getAtendenteResponsavel(), p.getPrazoEntrega(), p.getModalidadeEntrega(),
                    p.getCondicaoPagamento(), p.getObservacoes(), p.getStatus(), p.getStatus().permiteAlteracao(),
                    p.valorTotal(), p.custoEstimadoTotal(), p.getItens(), p.getHistorico(),
                    p.getCriadoEm(), p.getAtualizadoEm());
        }
    }

    private final RegistrarPedidoUseCase registrar;
    private final AlterarPedidoUseCase alterar;
    private final MovimentarPedidoUseCase movimentar;
    private final ConsultarPedidosUseCase consultar;

    PedidoController(RegistrarPedidoUseCase registrar, AlterarPedidoUseCase alterar,
                     MovimentarPedidoUseCase movimentar, ConsultarPedidosUseCase consultar) {
        this.registrar = registrar;
        this.alterar = alterar;
        this.movimentar = movimentar;
        this.consultar = consultar;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PedidoResponse registrar(@Valid @RequestBody NovoPedidoRequest r) {
        return PedidoResponse.from(registrar.executar(r.toCommand()));
    }

    @GetMapping
    List<PedidoResponse> listar(@RequestParam(required = false) StatusPedido status,
                                @RequestParam(required = false) UUID clienteId) {
        return consultar.listar(status, clienteId).stream().map(PedidoResponse::from).toList();
    }

    @GetMapping("/{id}")
    PedidoResponse buscar(@PathVariable UUID id) {
        return PedidoResponse.from(consultar.buscar(id));
    }

    /** Alteração — permitida apenas antes da entrada em produção (Regra de Ouro). */
    @PutMapping("/{id}")
    PedidoResponse alterar(@PathVariable UUID id, @Valid @RequestBody AlteracaoRequest r) {
        return PedidoResponse.from(alterar.executar(id, r.toCommand()));
    }

    @PostMapping("/{id}/reprocessamento-insumos")
    PedidoResponse reprocessarInsumos(@PathVariable UUID id) {
        return PedidoResponse.from(movimentar.reprocessarInsumos(id));
    }

    /** Integração: chamado pelo producao-service ao iniciar a ordem. */
    @PostMapping("/{id}/inicio-producao")
    PedidoResponse iniciarProducao(@PathVariable UUID id) {
        return PedidoResponse.from(movimentar.iniciarProducao(id));
    }

    /** Integração: chamado pelo producao-service ao concluir a ordem. */
    @PostMapping("/{id}/conclusao-producao")
    PedidoResponse finalizarProducao(@PathVariable UUID id) {
        return PedidoResponse.from(movimentar.finalizarProducao(id));
    }

    @PostMapping("/{id}/despacho")
    PedidoResponse despachar(@PathVariable UUID id) {
        return PedidoResponse.from(movimentar.despachar(id));
    }

    /** Validação na entrega: bloqueada enquanto houver saldo a receber. */
    @PostMapping("/{id}/entrega")
    PedidoResponse confirmarEntrega(@PathVariable UUID id) {
        return PedidoResponse.from(movimentar.confirmarEntrega(id));
    }

    @PostMapping("/{id}/cancelamento")
    PedidoResponse cancelar(@PathVariable UUID id, @RequestBody(required = false) CancelamentoRequest r) {
        return PedidoResponse.from(movimentar.cancelar(id, r == null ? null : r.motivo()));
    }
}
