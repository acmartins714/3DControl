package br.edu.uniesp.threedcontrol.financeiro.lucratividade.infrastructure.web;

import br.edu.uniesp.threedcontrol.financeiro.lucratividade.application.AnalisarLucratividadeUseCase;
import br.edu.uniesp.threedcontrol.financeiro.lucratividade.domain.AnaliseLucratividade.LucratividadeCategoria;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/financeiro/lucratividade")
class LucratividadeController {

    private final AnalisarLucratividadeUseCase useCase;

    LucratividadeController(AnalisarLucratividadeUseCase useCase) {
        this.useCase = useCase;
    }

    /** Receita, custo e margem por categoria de produto no período (padrão: mês corrente). */
    @GetMapping
    List<LucratividadeCategoria> porCategoria(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return useCase.porCategoria(inicio, fim);
    }
}
