package br.edu.uniesp.threedcontrol.estoque.estoque.application;

import br.edu.uniesp.threedcontrol.estoque.estoque.domain.EstoqueRepositories.ItemEstoqueRepository;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.EstoqueRepositories.MovimentacaoRepository;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.ItemEstoque;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.MovimentacaoEstoque;
import br.edu.uniesp.threedcontrol.estoque.shared.domain.DomainException;
import br.edu.uniesp.threedcontrol.estoque.shared.domain.RecursoNaoEncontradoException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class GerenciarEstoqueUseCase {

    private final ItemEstoqueRepository itens;
    private final MovimentacaoRepository movimentacoes;

    public GerenciarEstoqueUseCase(ItemEstoqueRepository itens, MovimentacaoRepository movimentacoes) {
        this.itens = itens;
        this.movimentacoes = movimentacoes;
    }

    /** Passa a controlar o saldo de um insumo do cadastro. */
    public ItemEstoque cadastrarItem(UUID insumoId, String nomeInsumo, String unidade, BigDecimal estoqueMinimo) {
        if (itens.buscarPorInsumoId(insumoId).isPresent()) {
            throw new DomainException("O insumo " + insumoId + " já possui controle de estoque.");
        }
        return itens.salvar(ItemEstoque.novo(insumoId, nomeInsumo, unidade, estoqueMinimo));
    }

    public ItemEstoque atualizarParametros(UUID insumoId, String nomeInsumo, String unidade, BigDecimal estoqueMinimo) {
        ItemEstoque item = buscar(insumoId);
        item.atualizarParametros(nomeInsumo, unidade, estoqueMinimo);
        return itens.salvar(item);
    }

    public ItemEstoque registrarEntrada(UUID insumoId, BigDecimal quantidade, String observacao) {
        ItemEstoque item = buscar(insumoId);
        item.registrarEntrada(quantidade);
        movimentacoes.registrar(MovimentacaoEstoque.de(insumoId, MovimentacaoEstoque.Tipo.ENTRADA, quantidade,
                null, observacao));
        return itens.salvar(item);
    }

    public ItemEstoque buscar(UUID insumoId) {
        return itens.buscarPorInsumoId(insumoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item de estoque do insumo", insumoId));
    }

    public List<ItemEstoque> listar() {
        return itens.listar();
    }

    /** Insumos cujo saldo livre está abaixo do mínimo: sinal para a administração comprar. */
    public List<ItemEstoque> listarAbaixoDoMinimo() {
        return itens.listar().stream().filter(ItemEstoque::abaixoDoMinimo).toList();
    }

    public List<MovimentacaoEstoque> movimentacoes(UUID insumoId) {
        return movimentacoes.listarPorInsumo(insumoId);
    }
}
