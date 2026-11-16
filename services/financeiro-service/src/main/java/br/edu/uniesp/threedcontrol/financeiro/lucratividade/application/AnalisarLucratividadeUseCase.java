package br.edu.uniesp.threedcontrol.financeiro.lucratividade.application;

import br.edu.uniesp.threedcontrol.financeiro.contareceber.domain.ContaReceberRepository;
import br.edu.uniesp.threedcontrol.financeiro.lucratividade.domain.AnaliseLucratividade;
import br.edu.uniesp.threedcontrol.financeiro.lucratividade.domain.AnaliseLucratividade.LucratividadeCategoria;

import java.time.LocalDate;
import java.util.List;

public class AnalisarLucratividadeUseCase {

    private final ContaReceberRepository contas;

    public AnalisarLucratividadeUseCase(ContaReceberRepository contas) {
        this.contas = contas;
    }

    public List<LucratividadeCategoria> porCategoria(LocalDate inicio, LocalDate fim) {
        LocalDate de = inicio != null ? inicio : LocalDate.now().withDayOfMonth(1);
        LocalDate ate = fim != null ? fim : LocalDate.now();
        return AnaliseLucratividade.porCategoria(
                contas.listarCriadasEntre(de.atStartOfDay(), ate.plusDays(1).atStartOfDay()));
    }
}
