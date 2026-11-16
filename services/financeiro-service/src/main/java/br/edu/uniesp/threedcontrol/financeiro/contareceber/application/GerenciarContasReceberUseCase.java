package br.edu.uniesp.threedcontrol.financeiro.contareceber.application;

import br.edu.uniesp.threedcontrol.financeiro.contareceber.domain.*;
import br.edu.uniesp.threedcontrol.financeiro.shared.domain.DomainException;
import br.edu.uniesp.threedcontrol.financeiro.shared.domain.RecursoNaoEncontradoException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class GerenciarContasReceberUseCase {

    /** Resposta usada pelo pedido-service para autorizar a entrega. */
    public record SituacaoFinanceira(UUID pedidoId, BigDecimal valorTotal, BigDecimal totalPago,
                                     BigDecimal saldoRemanescente, StatusConta status, boolean quitado) {
        static SituacaoFinanceira de(ContaReceber c) {
            return new SituacaoFinanceira(c.getPedidoId(), c.valorTotal(), c.totalPago(), c.saldoRemanescente(),
                    c.getStatus(), c.liberadaParaEntrega());
        }
    }

    private final ContaReceberRepository repository;

    public GerenciarContasReceberUseCase(ContaReceberRepository repository) {
        this.repository = repository;
    }

    public ContaReceber abrir(UUID pedidoId, UUID clienteId, List<ItemConta> itens) {
        if (repository.buscarPorPedidoId(pedidoId).isPresent()) {
            throw new DomainException("Já existe conta a receber para o pedido " + pedidoId + ".");
        }
        return repository.salvar(ContaReceber.abrir(pedidoId, clienteId, itens));
    }

    public ContaReceber atualizarItens(UUID pedidoId, List<ItemConta> itens) {
        ContaReceber conta = buscar(pedidoId);
        conta.atualizarItens(itens);
        return repository.salvar(conta);
    }

    public ContaReceber registrarPagamento(UUID pedidoId, BigDecimal valor, Pagamento.FormaPagamento forma, String obs) {
        ContaReceber conta = buscar(pedidoId);
        conta.registrarPagamento(valor, forma, obs);
        return repository.salvar(conta);
    }

    public void cancelar(UUID pedidoId) {
        repository.buscarPorPedidoId(pedidoId).ifPresent(c -> {
            c.cancelar();
            repository.salvar(c);
        });
    }

    public SituacaoFinanceira situacao(UUID pedidoId) {
        return SituacaoFinanceira.de(buscar(pedidoId));
    }

    public ContaReceber buscar(UUID pedidoId) {
        return repository.buscarPorPedidoId(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta a receber do pedido", pedidoId));
    }

    public List<ContaReceber> listar(StatusConta status) {
        return repository.listar(status);
    }
}
