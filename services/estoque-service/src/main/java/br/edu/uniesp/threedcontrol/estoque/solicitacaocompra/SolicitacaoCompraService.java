package br.edu.uniesp.threedcontrol.estoque.solicitacaocompra;

import br.edu.uniesp.threedcontrol.estoque.estoque.EstoqueService;
import br.edu.uniesp.threedcontrol.estoque.shared.RecursoNaoEncontradoException;
import br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.SolicitacaoCompraDtos.SolicitacaoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SolicitacaoCompraService {

    private final SolicitacaoCompraRepository repository;
    private final EstoqueService estoque;

    public SolicitacaoCompraService(SolicitacaoCompraRepository repository, EstoqueService estoque) {
        this.repository = repository;
        this.estoque = estoque;
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoResponse> listar(SolicitacaoCompra.Status status, UUID pedidoId) {
        List<SolicitacaoCompra> lista = status == null
                ? repository.findAllByOrderBySolicitadaEmDesc()
                : repository.findByStatusOrderBySolicitadaEm(status);
        return lista.stream()
                .filter(s -> pedidoId == null || pedidoId.equals(s.getPedidoId()))
                .map(SolicitacaoResponse::from)
                .toList();
    }

    /** Registra a chegada do material: dá entrada no estoque e encerra a solicitação. */
    public SolicitacaoResponse registrarRecebimento(UUID id, BigDecimal quantidadeRecebida) {
        SolicitacaoCompra s = buscarEntidade(id);
        s.registrarRecebimento();
        BigDecimal quantidade = quantidadeRecebida != null ? quantidadeRecebida : s.getQuantidade();
        estoque.registrarEntrada(s.getInsumoId(), quantidade, "Recebimento da solicitação de compra " + id);
        return SolicitacaoResponse.from(s);
    }

    public SolicitacaoResponse cancelar(UUID id) {
        SolicitacaoCompra s = buscarEntidade(id);
        s.cancelar();
        return SolicitacaoResponse.from(s);
    }

    private SolicitacaoCompra buscarEntidade(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação de compra", id));
    }
}
