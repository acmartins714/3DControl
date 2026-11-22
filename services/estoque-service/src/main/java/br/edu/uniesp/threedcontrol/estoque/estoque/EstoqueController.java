package br.edu.uniesp.threedcontrol.estoque.estoque;

import br.edu.uniesp.threedcontrol.estoque.estoque.EstoqueDtos.EntradaRequest;
import br.edu.uniesp.threedcontrol.estoque.estoque.EstoqueDtos.ItemEstoqueRequest;
import br.edu.uniesp.threedcontrol.estoque.estoque.EstoqueDtos.ItemEstoqueResponse;
import br.edu.uniesp.threedcontrol.estoque.estoque.EstoqueDtos.MovimentacaoResponse;
import br.edu.uniesp.threedcontrol.estoque.estoque.EstoqueDtos.ParametrosRequest;
import br.edu.uniesp.threedcontrol.estoque.estoque.EstoqueDtos.ReservaRequest;
import br.edu.uniesp.threedcontrol.estoque.estoque.EstoqueDtos.ResultadoReserva;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/estoque")
public class EstoqueController {

    private final EstoqueService estoque;
    private final ReservaService reservas;

    public EstoqueController(EstoqueService estoque, ReservaService reservas) {
        this.estoque = estoque;
        this.reservas = reservas;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemEstoqueResponse cadastrar(@Valid @RequestBody ItemEstoqueRequest request) {
        return estoque.cadastrarItem(request);
    }

    @GetMapping
    public List<ItemEstoqueResponse> listar() {
        return estoque.listar();
    }

    @GetMapping("/alertas")
    public List<ItemEstoqueResponse> abaixoDoMinimo() {
        return estoque.listarAbaixoDoMinimo();
    }

    @GetMapping("/{insumoId}")
    public ItemEstoqueResponse buscar(@PathVariable UUID insumoId) {
        return estoque.buscar(insumoId);
    }

    @PutMapping("/{insumoId}")
    public ItemEstoqueResponse atualizar(@PathVariable UUID insumoId, @Valid @RequestBody ParametrosRequest request) {
        return estoque.atualizarParametros(insumoId, request);
    }

    @PostMapping("/{insumoId}/entradas")
    public ItemEstoqueResponse registrarEntrada(@PathVariable UUID insumoId, @Valid @RequestBody EntradaRequest request) {
        return estoque.registrarEntrada(insumoId, request.quantidade(), request.observacao());
    }

    @GetMapping("/{insumoId}/movimentacoes")
    public List<MovimentacaoResponse> movimentacoes(@PathVariable UUID insumoId) {
        return estoque.movimentacoes(insumoId);
    }

    /** Usado pelo pedido-service ao registrar/alterar um pedido. */
    @PostMapping("/reservas")
    public ResultadoReserva reservar(@Valid @RequestBody ReservaRequest request) {
        return reservas.reservar(request.pedidoId(), request.itens());
    }

    /** Usado pelo pedido-service quando o pedido entra em produção. */
    @PostMapping("/reservas/{pedidoId}/consumo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void consumir(@PathVariable UUID pedidoId) {
        reservas.consumir(pedidoId);
    }

    /** Usado pelo pedido-service quando o pedido é cancelado. */
    @DeleteMapping("/reservas/{pedidoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void liberar(@PathVariable UUID pedidoId) {
        reservas.liberar(pedidoId);
    }
}
