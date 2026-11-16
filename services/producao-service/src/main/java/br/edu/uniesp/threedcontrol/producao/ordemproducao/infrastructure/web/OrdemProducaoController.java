package br.edu.uniesp.threedcontrol.producao.ordemproducao.infrastructure.web;

import br.edu.uniesp.threedcontrol.producao.ordemproducao.application.ExecutarProducaoUseCase;
import br.edu.uniesp.threedcontrol.producao.ordemproducao.application.PlanejarProducaoUseCase;
import br.edu.uniesp.threedcontrol.producao.ordemproducao.domain.OrdemProducao;
import br.edu.uniesp.threedcontrol.producao.ordemproducao.domain.Prioridade;
import br.edu.uniesp.threedcontrol.producao.ordemproducao.domain.StatusOrdem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/producao")
class OrdemProducaoController {

    record PlanejamentoRequest(@NotNull UUID pedidoId, String codigoPedido, @NotNull LocalDate prazoEntrega,
                               boolean insumosDisponiveis) {
    }

    record InicioRequest(@NotBlank String responsavel) {
    }

    record RetrabalhoRequest(@NotBlank String motivo) {
    }

    record OrdemResponse(UUID id, UUID pedidoId, String codigoPedido, LocalDate prazoEntrega, boolean insumosDisponiveis,
                         StatusOrdem status, Prioridade prioridade, boolean atrasada, String responsavel,
                         int retrabalhos, String observacoes, LocalDateTime criadaEm, LocalDateTime iniciadaEm,
                         LocalDateTime concluidaEm) {
        static OrdemResponse from(OrdemProducao o) {
            LocalDate hoje = LocalDate.now();
            return from(o, o.prioridade(hoje), o.estaAtrasada(hoje));
        }

        static OrdemResponse from(OrdemProducao o, Prioridade prioridade, boolean atrasada) {
            return new OrdemResponse(o.getId(), o.getPedidoId(), o.getCodigoPedido(), o.getPrazoEntrega(),
                    o.isInsumosDisponiveis(), o.getStatus(), prioridade, atrasada, o.getResponsavel(),
                    o.getRetrabalhos(), o.getObservacoes(), o.getCriadaEm(), o.getIniciadaEm(), o.getConcluidaEm());
        }
    }

    private final PlanejarProducaoUseCase planejamento;
    private final ExecutarProducaoUseCase execucao;

    OrdemProducaoController(PlanejarProducaoUseCase planejamento, ExecutarProducaoUseCase execucao) {
        this.planejamento = planejamento;
        this.execucao = execucao;
    }

    /** Fila de produção priorizada, consultada pela equipe fabril. */
    @GetMapping("/fila")
    List<OrdemResponse> fila() {
        return planejamento.fila().stream()
                .map(i -> OrdemResponse.from(i.ordem(), i.prioridade(), i.atrasada())).toList();
    }

    /** Usado pelo pedido-service: cria ou replaneja a ordem do pedido. */
    @PostMapping("/ordens")
    @ResponseStatus(HttpStatus.CREATED)
    OrdemResponse planejar(@Valid @RequestBody PlanejamentoRequest r) {
        return OrdemResponse.from(planejamento.planejar(r.pedidoId(), r.codigoPedido(), r.prazoEntrega(), r.insumosDisponiveis()));
    }

    @GetMapping("/ordens/{id}")
    OrdemResponse buscar(@PathVariable UUID id) {
        return OrdemResponse.from(planejamento.buscar(id));
    }

    @GetMapping("/ordens/pedido/{pedidoId}")
    OrdemResponse buscarPorPedido(@PathVariable UUID pedidoId) {
        return OrdemResponse.from(planejamento.buscarPorPedido(pedidoId));
    }

    @PostMapping("/ordens/{id}/inicio")
    OrdemResponse iniciar(@PathVariable UUID id, @Valid @RequestBody InicioRequest r) {
        return OrdemResponse.from(execucao.iniciar(id, r.responsavel()));
    }

    @PostMapping("/ordens/{id}/retrabalhos")
    OrdemResponse retrabalho(@PathVariable UUID id, @Valid @RequestBody RetrabalhoRequest r) {
        return OrdemResponse.from(execucao.registrarRetrabalho(id, r.motivo()));
    }

    @PostMapping("/ordens/{id}/conclusao")
    OrdemResponse concluir(@PathVariable UUID id) {
        return OrdemResponse.from(execucao.concluir(id));
    }

    /** Usado pelo pedido-service quando o pedido é cancelado. */
    @DeleteMapping("/ordens/pedido/{pedidoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancelarPorPedido(@PathVariable UUID pedidoId) {
        planejamento.cancelarPorPedido(pedidoId);
    }
}
