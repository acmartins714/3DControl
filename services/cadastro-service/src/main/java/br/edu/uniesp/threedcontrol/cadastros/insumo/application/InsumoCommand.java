package br.edu.uniesp.threedcontrol.cadastros.insumo.application;

import br.edu.uniesp.threedcontrol.cadastros.insumo.domain.UnidadeMedida;

import java.math.BigDecimal;

public record InsumoCommand(String nome, String descricao, UnidadeMedida unidadeMedida,
                            BigDecimal custoUnitario, boolean usoRegular) {
}
