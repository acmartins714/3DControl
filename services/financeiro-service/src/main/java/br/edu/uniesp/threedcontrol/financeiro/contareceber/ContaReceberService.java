package br.edu.uniesp.threedcontrol.financeiro.contareceber;

import br.edu.uniesp.threedcontrol.financeiro.contareceber.ContaReceberDtos.AberturaRequest;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.ContaReceberDtos.ContaResponse;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.ContaReceberDtos.ItemRequest;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.ContaReceberDtos.PagamentoRequest;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.ContaReceberDtos.SituacaoFinanceira;
import br.edu.uniesp.threedcontrol.financeiro.shared.RecursoNaoEncontradoException;
import br.edu.uniesp.threedcontrol.financeiro.shared.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ContaReceberService {

    private final ContaReceberRepository repository;

    public ContaReceberService(ContaReceberRepository repository) {
        this.repository = repository;
    }

    public ContaResponse abrir(AberturaRequest req) {
        if (repository.findByPedidoId(req.pedidoId()).isPresent()) {
            throw new RegraNegocioException("Já existe conta a receber para o pedido " + req.pedidoId() + ".");
        }
        ContaReceber conta = new ContaReceber(req.pedidoId(), req.clienteId(), ItemRequest.converter(req.itens()));
        return ContaResponse.from(repository.save(conta));
    }

    public ContaResponse atualizarItens(UUID pedidoId, List<ItemRequest> itens) {
        ContaReceber conta = buscarEntidade(pedidoId);
        conta.atualizarItens(ItemRequest.converter(itens));
        return ContaResponse.from(conta);
    }

    public ContaResponse registrarPagamento(UUID pedidoId, PagamentoRequest req) {
        ContaReceber conta = buscarEntidade(pedidoId);
        conta.registrarPagamento(req.valor(), req.forma(), req.observacao());
        return ContaResponse.from(conta);
    }

    public void cancelar(UUID pedidoId) {
        repository.findByPedidoId(pedidoId).ifPresent(ContaReceber::cancelar);
    }

    @Transactional(readOnly = true)
    public SituacaoFinanceira situacao(UUID pedidoId) {
        return SituacaoFinanceira.from(buscarEntidade(pedidoId));
    }

    @Transactional(readOnly = true)
    public ContaResponse buscar(UUID pedidoId) {
        return ContaResponse.from(buscarEntidade(pedidoId));
    }

    @Transactional(readOnly = true)
    public List<ContaResponse> listar(StatusConta status) {
        List<ContaReceber> contas = status == null
                ? repository.findAllByOrderByCriadaEmDesc()
                : repository.findByStatusOrderByCriadaEmDesc(status);
        return contas.stream().map(ContaResponse::from).toList();
    }

    private ContaReceber buscarEntidade(UUID pedidoId) {
        return repository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta a receber do pedido", pedidoId));
    }
}
