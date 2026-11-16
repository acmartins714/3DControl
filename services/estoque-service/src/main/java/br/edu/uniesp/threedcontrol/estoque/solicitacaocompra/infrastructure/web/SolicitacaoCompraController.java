package br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.infrastructure.web;

import br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.application.GerenciarSolicitacoesCompraUseCase;
import br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.domain.SolicitacaoCompra;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/solicitacoes-compra")
class SolicitacaoCompraController {

    record RecebimentoRequest(BigDecimal quantidadeRecebida) {
    }

    record SolicitacaoResponse(UUID id, UUID insumoId, String nomeInsumo, BigDecimal quantidade, UUID pedidoId,
                               SolicitacaoCompra.Status status, LocalDateTime solicitadaEm, LocalDateTime atualizadaEm) {
        static SolicitacaoResponse from(SolicitacaoCompra s) {
            return new SolicitacaoResponse(s.getId(), s.getInsumoId(), s.getNomeInsumo(), s.getQuantidade(),
                    s.getPedidoId(), s.getStatus(), s.getSolicitadaEm(), s.getAtualizadaEm());
        }
    }

    private final GerenciarSolicitacoesCompraUseCase useCase;

    SolicitacaoCompraController(GerenciarSolicitacoesCompraUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    List<SolicitacaoResponse> listar(@RequestParam(required = false) SolicitacaoCompra.Status status,
                                     @RequestParam(required = false) UUID pedidoId) {
        return useCase.listar(status, pedidoId).stream().map(SolicitacaoResponse::from).toList();
    }

    @PostMapping("/{id}/recebimento")
    SolicitacaoResponse receber(@PathVariable UUID id, @RequestBody(required = false) RecebimentoRequest r) {
        return SolicitacaoResponse.from(useCase.registrarRecebimento(id, r == null ? null : r.quantidadeRecebida()));
    }

    @PostMapping("/{id}/cancelamento")
    SolicitacaoResponse cancelar(@PathVariable UUID id) {
        return SolicitacaoResponse.from(useCase.cancelar(id));
    }
}
