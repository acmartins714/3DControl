package br.edu.uniesp.threedcontrol.estoque.estoque.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.EstoqueRepositories.ItemEstoqueRepository;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.EstoqueRepositories.MovimentacaoRepository;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.ItemEstoque;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.MovimentacaoEstoque;
import br.edu.uniesp.threedcontrol.estoque.shared.domain.DomainException;
import br.edu.uniesp.threedcontrol.estoque.shared.domain.RecursoNaoEncontradoException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
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

    @Transactional(readOnly = true)
    public ItemEstoque buscar(UUID insumoId) {
        return itens.buscarPorInsumoId(insumoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item de estoque do insumo", insumoId));
    }

    @Transactional(readOnly = true)
    public List<ItemEstoque> listar() {
        return itens.listar();
    }

    /** Insumos cujo saldo livre está abaixo do mínimo: sinal para a administração comprar. */
    @Transactional(readOnly = true)
    public List<ItemEstoque> listarAbaixoDoMinimo() {
        return itens.listar().stream().filter(ItemEstoque::abaixoDoMinimo).toList();
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoque> movimentacoes(UUID insumoId) {
        return movimentacoes.listarPorInsumo(insumoId);
    }
}
