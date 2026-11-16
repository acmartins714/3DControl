package br.edu.uniesp.threedcontrol.cadastros.produto.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.edu.uniesp.threedcontrol.cadastros.insumo.domain.Insumo;
import br.edu.uniesp.threedcontrol.cadastros.insumo.domain.InsumoRepository;
import br.edu.uniesp.threedcontrol.cadastros.produto.domain.ComposicaoProduto;
import br.edu.uniesp.threedcontrol.cadastros.produto.domain.Produto;
import br.edu.uniesp.threedcontrol.cadastros.produto.domain.ProdutoRepository;
import br.edu.uniesp.threedcontrol.cadastros.shared.domain.DomainException;
import br.edu.uniesp.threedcontrol.cadastros.shared.domain.RecursoNaoEncontradoException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class GerenciarProdutosUseCase {

    private final ProdutoRepository produtos;
    private final InsumoRepository insumos;

    public GerenciarProdutosUseCase(ProdutoRepository produtos, InsumoRepository insumos) {
        this.produtos = produtos;
        this.insumos = insumos;
    }

    public ProdutoDetalhado cadastrar(ProdutoCommand cmd) {
        garantirInsumosValidos(cmd.composicao());
        Produto produto = Produto.novo(cmd.nome(), cmd.descricao(), cmd.categoria(), cmd.especificacoesTecnicas(),
                cmd.tempoMedioFabricacaoHoras(), cmd.precoBase(), cmd.composicao());
        return detalhar(produtos.salvar(produto));
    }

    public ProdutoDetalhado atualizar(UUID id, ProdutoCommand cmd) {
        Produto produto = buscarProduto(id);
        garantirInsumosValidos(cmd.composicao());
        produto.atualizar(cmd.nome(), cmd.descricao(), cmd.categoria(), cmd.especificacoesTecnicas(),
                cmd.tempoMedioFabricacaoHoras(), cmd.precoBase(), cmd.composicao());
        return detalhar(produtos.salvar(produto));
    }

    @Transactional(readOnly = true)
    public ProdutoDetalhado buscar(UUID id) {
        return detalhar(buscarProduto(id));
    }

    @Transactional(readOnly = true)
    public List<Produto> listar(String categoria) {
        return produtos.listar(categoria);
    }

    public void inativar(UUID id) {
        Produto produto = buscarProduto(id);
        produto.inativar();
        produtos.salvar(produto);
    }

    private Produto buscarProduto(UUID id) {
        return produtos.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));
    }

    private void garantirInsumosValidos(List<ComposicaoProduto> composicao) {
        if (composicao == null || composicao.isEmpty()) {
            return;
        }
        Set<UUID> ids = composicao.stream().map(ComposicaoProduto::insumoId).collect(Collectors.toSet());
        Map<UUID, Insumo> encontrados = insumos.buscarPorIds(ids).stream()
                .collect(Collectors.toMap(Insumo::getId, Function.identity()));
        for (UUID id : ids) {
            Insumo insumo = encontrados.get(id);
            if (insumo == null) {
                throw new DomainException("Insumo inexistente na composição: " + id);
            }
            if (!insumo.isAtivo()) {
                throw new DomainException("O insumo '" + insumo.getNome() + "' está inativo.");
            }
        }
    }

    private ProdutoDetalhado detalhar(Produto produto) {
        Map<UUID, Insumo> mapa = insumos.buscarPorIds(produto.idsInsumos()).stream()
                .collect(Collectors.toMap(Insumo::getId, Function.identity()));
        List<ProdutoDetalhado.InsumoDaComposicao> itens = new ArrayList<>();
        Map<UUID, java.math.BigDecimal> custos = new HashMap<>();
        for (ComposicaoProduto c : produto.getComposicao()) {
            Insumo i = mapa.get(c.insumoId());
            if (i != null) {
                custos.put(i.getId(), i.getCustoUnitario());
                itens.add(new ProdutoDetalhado.InsumoDaComposicao(i.getId(), i.getNome(), i.getUnidadeMedida(),
                        c.quantidade(), i.getCustoUnitario()));
            }
        }
        return new ProdutoDetalhado(produto, itens, produto.calcularCustoInsumos(custos));
    }
}
