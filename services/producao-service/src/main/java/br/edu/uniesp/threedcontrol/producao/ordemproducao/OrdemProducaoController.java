package br.edu.uniesp.threedcontrol.producao.ordemproducao;

import br.edu.uniesp.threedcontrol.producao.ordemproducao.OrdemProducaoDtos.InicioRequest;
import br.edu.uniesp.threedcontrol.producao.ordemproducao.OrdemProducaoDtos.OrdemResponse;
import br.edu.uniesp.threedcontrol.producao.ordemproducao.OrdemProducaoDtos.PlanejamentoRequest;
import br.edu.uniesp.threedcontrol.producao.ordemproducao.OrdemProducaoDtos.RetrabalhoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/producao")
public class OrdemProducaoController {

    private final OrdemProducaoService service;

    public OrdemProducaoController(OrdemProducaoService service) {
        this.service = service;
    }

    /** Fila de produção priorizada, consultada pela equipe fabril. */
    @GetMapping("/fila")
    public List<OrdemResponse> fila() {
        return service.fila();
    }

    /** Usado pelo pedido-service: cria ou replaneja a ordem do pedido. */
    @PostMapping("/ordens")
    @ResponseStatus(HttpStatus.CREATED)
    public OrdemResponse planejar(@Valid @RequestBody PlanejamentoRequest request) {
        return service.planejar(request);
    }

    @GetMapping("/ordens/{id}")
    public OrdemResponse buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @GetMapping("/ordens/pedido/{pedidoId}")
    public OrdemResponse buscarPorPedido(@PathVariable UUID pedidoId) {
        return service.buscarPorPedido(pedidoId);
    }

    @PostMapping("/ordens/{id}/inicio")
    public OrdemResponse iniciar(@PathVariable UUID id, @Valid @RequestBody InicioRequest request) {
        return service.iniciar(id, request.responsavel());
    }

    @PostMapping("/ordens/{id}/retrabalhos")
    public OrdemResponse retrabalho(@PathVariable UUID id, @Valid @RequestBody RetrabalhoRequest request) {
        return service.registrarRetrabalho(id, request.motivo());
    }

    @PostMapping("/ordens/{id}/conclusao")
    public OrdemResponse concluir(@PathVariable UUID id) {
        return service.concluir(id);
    }

    /** Usado pelo pedido-service quando o pedido é cancelado. */
    @DeleteMapping("/ordens/pedido/{pedidoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelarPorPedido(@PathVariable UUID pedidoId) {
        service.cancelarPorPedido(pedidoId);
    }
}
