package br.edu.uniesp.threedcontrol.producao.ordemproducao.domain;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Serviço de domínio que ordena a fila de produção:
 * 1) ordens com insumos disponíveis primeiro; 2) maior prioridade de entrega;
 * 3) prazo mais próximo; 4) ordem de chegada.
 */
public final class FilaProducao {

    private FilaProducao() {
    }

    public static List<OrdemProducao> ordenar(List<OrdemProducao> ordens, LocalDate hoje) {
        Comparator<OrdemProducao> comparador = Comparator
                .comparing(OrdemProducao::isInsumosDisponiveis).reversed()
                .thenComparing(o -> o.prioridade(hoje).peso(), Comparator.reverseOrder())
                .thenComparing(OrdemProducao::getPrazoEntrega)
                .thenComparing(OrdemProducao::getCriadaEm);
        return ordens.stream().sorted(comparador).toList();
    }
}
