package br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.application;

import br.edu.uniesp.threedcontrol.estoque.estoque.application.GerenciarEstoqueUseCase;
import br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.domain.SolicitacaoCompra;
import br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.domain.SolicitacaoCompraRepository;
import br.edu.uniesp.threedcontrol.estoque.shared.domain.RecursoNaoEncontradoException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class GerenciarSolicitacoesCompraUseCase {

    private final SolicitacaoCompraRepository repository;
    private final GerenciarEstoqueUseCase estoque;

    public GerenciarSolicitacoesCompraUseCase(SolicitacaoCompraRepository repository, GerenciarEstoqueUseCase estoque) {
        this.repository = repository;
        this.estoque = estoque;
    }

    public List<SolicitacaoCompra> listar(SolicitacaoCompra.Status status) {
        return repository.listar(status);
    }

    /** Registra a chegada do material: dá entrada no estoque e encerra a solicitação. */
    public SolicitacaoCompra registrarRecebimento(UUID id, BigDecimal quantidadeRecebida) {
        SolicitacaoCompra s = buscar(id);
        s.registrarRecebimento();
        BigDecimal quantidade = quantidadeRecebida != null ? quantidadeRecebida : s.getQuantidade();
        estoque.registrarEntrada(s.getInsumoId(), quantidade, "Recebimento da solicitação de compra " + id);
        return repository.salvar(s);
    }

    public SolicitacaoCompra cancelar(UUID id) {
        SolicitacaoCompra s = buscar(id);
        s.cancelar();
        return repository.salvar(s);
    }

    private SolicitacaoCompra buscar(UUID id) {
        return repository.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação de compra", id));
    }
}
