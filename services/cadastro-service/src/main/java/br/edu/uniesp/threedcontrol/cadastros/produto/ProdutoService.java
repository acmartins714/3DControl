package br.edu.uniesp.threedcontrol.cadastros.produto;

import br.edu.uniesp.threedcontrol.cadastros.insumo.Insumo;
import br.edu.uniesp.threedcontrol.cadastros.insumo.InsumoRepository;
import br.edu.uniesp.threedcontrol.cadastros.produto.ProdutoDtos.ComposicaoResponse;
import br.edu.uniesp.threedcontrol.cadastros.produto.ProdutoDtos.ProdutoRequest;
import br.edu.uniesp.threedcontrol.cadastros.produto.ProdutoDtos.ProdutoResponse;
import br.edu.uniesp.threedcontrol.cadastros.produto.ProdutoDtos.ProdutoResumoResponse;
import br.edu.uniesp.threedcontrol.cadastros.shared.RecursoNaoEncontradoException;
import br.edu.uniesp.threedcontrol.cadastros.shared.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProdutoService {

    private final ProdutoRepository produtos;
    private final InsumoRepository insumos;

    public ProdutoService(ProdutoRepository produtos, InsumoRepository insumos) {
        this.produtos = produtos;
        this.insumos = insumos;
    }

    public ProdutoResponse cadastrar(ProdutoRequest req) {
        List<ItemComposicao> composicao = req.itensComposicao();
        garantirInsumosValidos(composicao);
        Produto produto = new Produto(req.nome(), req.descricao(), req.categoria(), req.especificacoesTecnicas(),
                req.tempoMedioFabricacaoHoras(), req.precoBase(), composicao);
        return detalhar(produtos.save(produto));
    }

    public ProdutoResponse atualizar(UUID id, ProdutoRequest req) {
        Produto produto = buscarEntidade(id);
        List<ItemComposicao> composicao = req.itensComposicao();
        garantirInsumosValidos(composicao);
        produto.atualizar(req.nome(), req.descricao(), req.categoria(), req.especificacoesTecnicas(),
                req.tempoMedioFabricacaoHoras(), req.precoBase(), composicao);
        return detalhar(produto);
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscar(UUID id) {
        return detalhar(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<ProdutoResumoResponse> listar(String categoria) {
        List<Produto> lista = (categoria == null || categoria.isBlank())
                ? produtos.findAllByOrderByNome()
                : produtos.findByCategoriaIgnoreCaseOrderByNome(categoria);
        return lista.stream().map(ProdutoResumoResponse::from).toList();
    }

    public void inativar(UUID id) {
        buscarEntidade(id).inativar();
    }

    private Produto buscarEntidade(UUID id) {
        return produtos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));
    }

    private void garantirInsumosValidos(List<ItemComposicao> composicao) {
        Set<UUID> ids = composicao.stream().map(ItemComposicao::getInsumoId).collect(Collectors.toSet());
        Map<UUID, Insumo> encontrados = insumosPorId(ids);
        for (UUID id : ids) {
            Insumo insumo = encontrados.get(id);
            if (insumo == null) {
                throw new RegraNegocioException("Insumo inexistente na composição: " + id);
            }
            if (!insumo.isAtivo()) {
                throw new RegraNegocioException("O insumo '" + insumo.getNome() + "' está inativo.");
            }
        }
    }

    private ProdutoResponse detalhar(Produto p) {
        Map<UUID, Insumo> mapa = insumosPorId(p.idsInsumos());
        List<ComposicaoResponse> itens = new ArrayList<>();
        Map<UUID, BigDecimal> custos = new HashMap<>();
        for (ItemComposicao c : p.getComposicao()) {
            Insumo i = mapa.get(c.getInsumoId());
            if (i != null) {
                custos.put(i.getId(), i.getCustoUnitario());
                itens.add(new ComposicaoResponse(i.getId(), i.getNome(), i.getUnidadeMedida(),
                        c.getQuantidade(), i.getCustoUnitario()));
            }
        }
        return new ProdutoResponse(p.getId(), p.getNome(), p.getDescricao(), p.getCategoria(),
                p.getEspecificacoesTecnicas(), p.getTempoMedioFabricacaoHoras(), p.getPrecoBase(),
                p.calcularCustoInsumos(custos), p.isAtivo(), itens);
    }

    private Map<UUID, Insumo> insumosPorId(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return insumos.findAllById(ids).stream().collect(Collectors.toMap(Insumo::getId, Function.identity()));
    }
}
