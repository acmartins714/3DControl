package br.edu.uniesp.threedcontrol.estoque.estoque;

import br.edu.uniesp.threedcontrol.estoque.estoque.EstoqueDtos.ItemEstoqueRequest;
import br.edu.uniesp.threedcontrol.estoque.estoque.EstoqueDtos.ItemEstoqueResponse;
import br.edu.uniesp.threedcontrol.estoque.estoque.EstoqueDtos.MovimentacaoResponse;
import br.edu.uniesp.threedcontrol.estoque.estoque.EstoqueDtos.ParametrosRequest;
import br.edu.uniesp.threedcontrol.estoque.shared.RecursoNaoEncontradoException;
import br.edu.uniesp.threedcontrol.estoque.shared.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Cadastro dos itens controlados, entradas de material e consultas de saldo. */
@Service
@Transactional
public class EstoqueService {

    private final ItemEstoqueRepository itens;
    private final MovimentacaoEstoqueRepository movimentacoes;

    public EstoqueService(ItemEstoqueRepository itens, MovimentacaoEstoqueRepository movimentacoes) {
        this.itens = itens;
        this.movimentacoes = movimentacoes;
    }

    /** Passa a controlar o saldo de um insumo do cadastro. */
    public ItemEstoqueResponse cadastrarItem(ItemEstoqueRequest req) {
        if (itens.findByInsumoId(req.insumoId()).isPresent()) {
            throw new RegraNegocioException("O insumo " + req.insumoId() + " já possui controle de estoque.");
        }
        ItemEstoque item = new ItemEstoque(req.insumoId(), req.nomeInsumo(), req.unidadeMedida(), req.estoqueMinimo());
        return ItemEstoqueResponse.from(itens.save(item));
    }

    public ItemEstoqueResponse atualizarParametros(UUID insumoId, ParametrosRequest req) {
        ItemEstoque item = buscarEntidade(insumoId);
        item.atualizarParametros(req.nomeInsumo(), req.unidadeMedida(), req.estoqueMinimo());
        return ItemEstoqueResponse.from(item);
    }

    public ItemEstoqueResponse registrarEntrada(UUID insumoId, BigDecimal quantidade, String observacao) {
        ItemEstoque item = buscarEntidade(insumoId);
        item.registrarEntrada(quantidade);
        movimentacoes.save(new MovimentacaoEstoque(insumoId, MovimentacaoEstoque.Tipo.ENTRADA, quantidade,
                null, observacao));
        return ItemEstoqueResponse.from(item);
    }

    @Transactional(readOnly = true)
    public ItemEstoqueResponse buscar(UUID insumoId) {
        return ItemEstoqueResponse.from(buscarEntidade(insumoId));
    }

    @Transactional(readOnly = true)
    public List<ItemEstoqueResponse> listar() {
        return itens.findAllByOrderByNomeInsumo().stream().map(ItemEstoqueResponse::from).toList();
    }

    /** Insumos cujo saldo livre está abaixo do mínimo: sinal para a administração comprar. */
    @Transactional(readOnly = true)
    public List<ItemEstoqueResponse> listarAbaixoDoMinimo() {
        return itens.findAllByOrderByNomeInsumo().stream()
                .filter(ItemEstoque::abaixoDoMinimo)
                .map(ItemEstoqueResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoResponse> movimentacoes(UUID insumoId) {
        return movimentacoes.findByInsumoIdOrderByDataHoraDesc(insumoId).stream()
                .map(MovimentacaoResponse::from).toList();
    }

    private ItemEstoque buscarEntidade(UUID insumoId) {
        return itens.findByInsumoId(insumoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item de estoque do insumo", insumoId));
    }
}
