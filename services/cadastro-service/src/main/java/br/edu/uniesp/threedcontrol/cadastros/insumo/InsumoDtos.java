package br.edu.uniesp.threedcontrol.cadastros.insumo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/** Formatos de entrada e saída da API de insumos. */
public final class InsumoDtos {

    private InsumoDtos() {
    }

    public record InsumoRequest(@NotBlank @Size(max = 120) String nome,
                                String descricao,
                                @NotNull UnidadeMedida unidadeMedida,
                                @NotNull @DecimalMin("0.0") BigDecimal custoUnitario,
                                boolean usoRegular) {
    }

    public record InsumoResponse(UUID id, String nome, String descricao, UnidadeMedida unidadeMedida,
                                 BigDecimal custoUnitario, boolean usoRegular, boolean ativo) {

        static InsumoResponse from(Insumo i) {
            return new InsumoResponse(i.getId(), i.getNome(), i.getDescricao(), i.getUnidadeMedida(),
                    i.getCustoUnitario(), i.isUsoRegular(), i.isAtivo());
        }
    }
}
