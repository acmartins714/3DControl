package br.edu.uniesp.threedcontrol.cadastros.produto;

import br.edu.uniesp.threedcontrol.cadastros.insumo.UnidadeMedida;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Formatos de entrada e saída da API de produtos. */
public final class ProdutoDtos {

    private ProdutoDtos() {
    }

    public record ComposicaoRequest(@NotNull UUID insumoId,
                                    @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantidade) {
    }

    public record ProdutoRequest(@NotBlank @Size(max = 150) String nome,
                                 String descricao,
                                 @NotBlank @Size(max = 80) String categoria,
                                 String especificacoesTecnicas,
                                 @PositiveOrZero int tempoMedioFabricacaoHoras,
                                 @NotNull @DecimalMin("0.0") BigDecimal precoBase,
                                 List<@Valid ComposicaoRequest> composicao) {

        List<ItemComposicao> itensComposicao() {
            return composicao == null ? List.of()
                    : composicao.stream().map(c -> new ItemComposicao(c.insumoId(), c.quantidade())).toList();
        }
    }

    public record ComposicaoResponse(UUID insumoId, String nomeInsumo, UnidadeMedida unidadeMedida,
                                     BigDecimal quantidade, BigDecimal custoUnitario) {
    }

    /** Produto com a ficha técnica resolvida (nome e custo de cada insumo) e o custo estimado por unidade. */
    public record ProdutoResponse(UUID id, String nome, String descricao, String categoria, String especificacoesTecnicas,
                                  int tempoMedioFabricacaoHoras, BigDecimal precoBase, BigDecimal custoEstimado,
                                  boolean ativo, List<ComposicaoResponse> composicao) {
    }

    public record ProdutoResumoResponse(UUID id, String nome, String categoria, BigDecimal precoBase, boolean ativo) {

        static ProdutoResumoResponse from(Produto p) {
            return new ProdutoResumoResponse(p.getId(), p.getNome(), p.getCategoria(), p.getPrecoBase(), p.isAtivo());
        }
    }
}
