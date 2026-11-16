package br.edu.uniesp.threedcontrol.producao.ordemproducao.domain;

import br.edu.uniesp.threedcontrol.producao.shared.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilaProducaoTest {

    private final LocalDate hoje = LocalDate.of(2026, 12, 1);

    private OrdemProducao ordem(String codigo, int diasParaPrazo, boolean insumos) {
        return OrdemProducao.nova(UUID.randomUUID(), codigo, hoje.plusDays(diasParaPrazo), insumos);
    }

    @Test
    void ordenaPorInsumosDisponiveisDepoisPrioridadeDePrazo() {
        OrdemProducao semInsumoUrgente = ordem("A", 1, false);
        OrdemProducao folgada = ordem("B", 15, true);
        OrdemProducao urgente = ordem("C", 1, true);
        OrdemProducao alta = ordem("D", 3, true);

        List<OrdemProducao> fila = FilaProducao.ordenar(List.of(semInsumoUrgente, folgada, urgente, alta), hoje);

        assertThat(fila).extracting(OrdemProducao::getCodigoPedido).containsExactly("C", "D", "B", "A");
    }

    @Test
    void prioridadeDerivadaDoPrazo() {
        assertThat(Prioridade.para(hoje.plusDays(1), hoje)).isEqualTo(Prioridade.URGENTE);
        assertThat(Prioridade.para(hoje.plusDays(3), hoje)).isEqualTo(Prioridade.ALTA);
        assertThat(Prioridade.para(hoje.plusDays(7), hoje)).isEqualTo(Prioridade.NORMAL);
        assertThat(Prioridade.para(hoje.plusDays(8), hoje)).isEqualTo(Prioridade.BAIXA);
    }

    @Test
    void naoIniciaOrdemQueAguardaInsumos() {
        OrdemProducao o = ordem("X", 5, false);

        assertThatThrownBy(() -> o.iniciar("Carlos")).isInstanceOf(DomainException.class);
    }

    @Test
    void ordemEmProducaoNaoPodeSerReplanejada() {
        OrdemProducao o = ordem("X", 5, true);
        o.iniciar("Carlos");

        assertThatThrownBy(() -> o.replanejar(hoje.plusDays(10), true)).isInstanceOf(DomainException.class);
    }
}
