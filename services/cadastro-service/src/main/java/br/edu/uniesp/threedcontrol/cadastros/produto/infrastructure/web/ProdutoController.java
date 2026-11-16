package br.edu.uniesp.threedcontrol.cadastros.produto.infrastructure.web;

import br.edu.uniesp.threedcontrol.cadastros.insumo.domain.UnidadeMedida;
import br.edu.uniesp.threedcontrol.cadastros.produto.application.GerenciarProdutosUseCase;
import br.edu.uniesp.threedcontrol.cadastros.produto.application.ProdutoCommand;
import br.edu.uniesp.threedcontrol.cadastros.produto.application.ProdutoDetalhado;
import br.edu.uniesp.threedcontrol.cadastros.produto.domain.ComposicaoProduto;
import br.edu.uniesp.threedcontrol.cadastros.produto.domain.Produto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/produtos")
class ProdutoController {

    record ComposicaoRequest(@NotNull UUID insumoId, @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantidade) {
    }

    record ProdutoRequest(@NotBlank @Size(max = 150) String nome,
                          String descricao,
                          @NotBlank @Size(max = 80) String categoria,
                          String especificacoesTecnicas,
                          @PositiveOrZero int tempoMedioFabricacaoHoras,
                          @NotNull @DecimalMin("0.0") BigDecimal precoBase,
                          List<@Valid ComposicaoRequest> composicao) {
        ProdutoCommand toCommand() {
            List<ComposicaoProduto> itens = composicao == null ? List.of()
                    : composicao.stream().map(c -> new ComposicaoProduto(c.insumoId(), c.quantidade())).toList();
            return new ProdutoCommand(nome, descricao, categoria, especificacoesTecnicas,
                    tempoMedioFabricacaoHoras, precoBase, itens);
        }
    }

    record ComposicaoResponse(UUID insumoId, String nomeInsumo, UnidadeMedida unidadeMedida,
                              BigDecimal quantidade, BigDecimal custoUnitario) {
    }

    record ProdutoResponse(UUID id, String nome, String descricao, String categoria, String especificacoesTecnicas,
                           int tempoMedioFabricacaoHoras, BigDecimal precoBase, BigDecimal custoEstimado,
                           boolean ativo, List<ComposicaoResponse> composicao) {
        static ProdutoResponse from(ProdutoDetalhado d) {
            Produto p = d.produto();
            return new ProdutoResponse(p.getId(), p.getNome(), p.getDescricao(), p.getCategoria(),
                    p.getEspecificacoesTecnicas(), p.getTempoMedioFabricacaoHoras(), p.getPrecoBase(),
                    d.custoEstimado(), p.isAtivo(),
                    d.composicao().stream().map(c -> new ComposicaoResponse(c.insumoId(), c.nome(),
                            c.unidadeMedida(), c.quantidade(), c.custoUnitario())).toList());
        }
    }

    record ProdutoResumoResponse(UUID id, String nome, String categoria, BigDecimal precoBase, boolean ativo) {
        static ProdutoResumoResponse from(Produto p) {
            return new ProdutoResumoResponse(p.getId(), p.getNome(), p.getCategoria(), p.getPrecoBase(), p.isAtivo());
        }
    }

    private final GerenciarProdutosUseCase useCase;

    ProdutoController(GerenciarProdutosUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProdutoResponse cadastrar(@Valid @RequestBody ProdutoRequest request) {
        return ProdutoResponse.from(useCase.cadastrar(request.toCommand()));
    }

    @GetMapping
    List<ProdutoResumoResponse> listar(@RequestParam(required = false) String categoria) {
        return useCase.listar(categoria).stream().map(ProdutoResumoResponse::from).toList();
    }

    @GetMapping("/{id}")
    ProdutoResponse buscar(@PathVariable UUID id) {
        return ProdutoResponse.from(useCase.buscar(id));
    }

    @PutMapping("/{id}")
    ProdutoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody ProdutoRequest request) {
        return ProdutoResponse.from(useCase.atualizar(id, request.toCommand()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void inativar(@PathVariable UUID id) {
        useCase.inativar(id);
    }
}
