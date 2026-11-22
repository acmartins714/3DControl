package br.edu.uniesp.threedcontrol.financeiro.lucratividade;

import br.edu.uniesp.threedcontrol.financeiro.lucratividade.LucratividadeService.LucratividadeCategoria;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/financeiro/lucratividade")
public class LucratividadeController {

    private final LucratividadeService service;

    public LucratividadeController(LucratividadeService service) {
        this.service = service;
    }

    /** Receita, custo e margem por categoria de produto no período (padrão: mês corrente). */
    @GetMapping
    public List<LucratividadeCategoria> porCategoria(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return service.porCategoria(inicio, fim);
    }
}
