package br.edu.uniesp.threedcontrol.estoque.estoque.infrastructure.web;

import br.edu.uniesp.threedcontrol.estoque.estoque.application.GerenciarEstoqueUseCase;
import br.edu.uniesp.threedcontrol.estoque.estoque.application.ReservarInsumosUseCase;
import br.edu.uniesp.threedcontrol.estoque.estoque.application.ResultadoReserva;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.ItemEstoque;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.MovimentacaoEstoque;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.NecessidadeInsumo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/estoque")
class EstoqueController {

    record ItemEstoqueRequest(@NotNull UUID insumoId, String nomeInsumo, String unidadeMedida,
                              @NotNull @DecimalMin("0.0") BigDecimal estoqueMinimo) {
    }

    record ParametrosRequest(String nomeInsumo, String unidadeMedida, @NotNull @DecimalMin("0.0") BigDecimal estoqueMinimo) {
    }

    record EntradaRequest(@NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantidade, String observacao) {
    }

    record NecessidadeRequest(@NotNull UUID insumoId, @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantidade) {
    }

    record ReservaRequest(@NotNull UUID pedidoId, @NotEmpty List<@Valid NecessidadeRequest> itens) {
    }

    record ItemEstoqueResponse(UUID insumoId, String nomeInsumo, String unidadeMedida, BigDecimal quantidadeDisponivel,
                               BigDecimal quantidadeReservada, BigDecimal saldoLivre, BigDecimal estoqueMinimo,
                               boolean abaixoDoMinimo) {
        static ItemEstoqueResponse from(ItemEstoque i) {
            return new ItemEstoqueResponse(i.getInsumoId(), i.getNomeInsumo(), i.getUnidadeMedida(),
                    i.getQuantidadeDisponivel(), i.getQuantidadeReservada(), i.saldoLivre(), i.getEstoqueMinimo(),
                    i.abaixoDoMinimo());
        }
    }

    private final GerenciarEstoqueUseCase estoque;
    private final ReservarInsumosUseCase reservas;

    EstoqueController(GerenciarEstoqueUseCase estoque, ReservarInsumosUseCase reservas) {
        this.estoque = estoque;
        this.reservas = reservas;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ItemEstoqueResponse cadastrar(@Valid @RequestBody ItemEstoqueRequest r) {
        return ItemEstoqueResponse.from(estoque.cadastrarItem(r.insumoId(), r.nomeInsumo(), r.unidadeMedida(), r.estoqueMinimo()));
    }

    @GetMapping
    List<ItemEstoqueResponse> listar() {
        return estoque.listar().stream().map(ItemEstoqueResponse::from).toList();
    }

    @GetMapping("/alertas")
    List<ItemEstoqueResponse> abaixoDoMinimo() {
        return estoque.listarAbaixoDoMinimo().stream().map(ItemEstoqueResponse::from).toList();
    }

    @GetMapping("/{insumoId}")
    ItemEstoqueResponse buscar(@PathVariable UUID insumoId) {
        return ItemEstoqueResponse.from(estoque.buscar(insumoId));
    }

    @PutMapping("/{insumoId}")
    ItemEstoqueResponse atualizar(@PathVariable UUID insumoId, @Valid @RequestBody ParametrosRequest r) {
        return ItemEstoqueResponse.from(estoque.atualizarParametros(insumoId, r.nomeInsumo(), r.unidadeMedida(), r.estoqueMinimo()));
    }

    @PostMapping("/{insumoId}/entradas")
    ItemEstoqueResponse registrarEntrada(@PathVariable UUID insumoId, @Valid @RequestBody EntradaRequest r) {
        return ItemEstoqueResponse.from(estoque.registrarEntrada(insumoId, r.quantidade(), r.observacao()));
    }

    @GetMapping("/{insumoId}/movimentacoes")
    List<MovimentacaoEstoque> movimentacoes(@PathVariable UUID insumoId) {
        return estoque.movimentacoes(insumoId);
    }

    /** Usado pelo pedido-service ao registrar/alterar um pedido. */
    @PostMapping("/reservas")
    ResultadoReserva reservar(@Valid @RequestBody ReservaRequest r) {
        List<NecessidadeInsumo> itens = r.itens().stream()
                .map(n -> new NecessidadeInsumo(n.insumoId(), n.quantidade())).toList();
        return reservas.reservar(r.pedidoId(), itens);
    }

    /** Usado pelo pedido-service quando o pedido entra em produção. */
    @PostMapping("/reservas/{pedidoId}/consumo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void consumir(@PathVariable UUID pedidoId) {
        reservas.consumir(pedidoId);
    }

    /** Usado pelo pedido-service quando o pedido é cancelado. */
    @DeleteMapping("/reservas/{pedidoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void liberar(@PathVariable UUID pedidoId) {
        reservas.liberar(pedidoId);
    }
}
