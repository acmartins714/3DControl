package br.edu.uniesp.threedcontrol.financeiro.lucratividade;

import br.edu.uniesp.threedcontrol.financeiro.contareceber.ContaReceber;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.ContaReceberRepository;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.ItemConta;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.StatusConta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Consolida receita, custo e margem por categoria de produto. */
@Service
@Transactional(readOnly = true)
public class LucratividadeService {

    public record LucratividadeCategoria(String categoria, long quantidadeItens, BigDecimal receita,
                                         BigDecimal custo, BigDecimal lucro, BigDecimal margemPercentual) {
    }

    private final ContaReceberRepository contas;

    public LucratividadeService(ContaReceberRepository contas) {
        this.contas = contas;
    }

    /** Período padrão: do primeiro dia do mês corrente até hoje. */
    public List<LucratividadeCategoria> porCategoria(LocalDate inicio, LocalDate fim) {
        LocalDate de = inicio != null ? inicio : LocalDate.now().withDayOfMonth(1);
        LocalDate ate = fim != null ? fim : LocalDate.now();
        return calcular(contas.findByCriadaEmBetween(de.atStartOfDay(), ate.plusDays(1).atStartOfDay()));
    }

    /** Cálculo isolado da consulta ao banco para poder ser testado diretamente. */
    static List<LucratividadeCategoria> calcular(Collection<ContaReceber> contas) {
        Map<String, BigDecimal[]> acumulado = new TreeMap<>();
        Map<String, Long> quantidade = new HashMap<>();
        for (ContaReceber conta : contas) {
            if (conta.getStatus() == StatusConta.CANCELADA) {
                continue;
            }
            for (ItemConta item : conta.getItens()) {
                BigDecimal[] valores = acumulado.computeIfAbsent(item.getCategoria(),
                        k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                valores[0] = valores[0].add(item.getValorVenda());
                valores[1] = valores[1].add(item.getCustoEstimado());
                quantidade.merge(item.getCategoria(), 1L, Long::sum);
            }
        }
        List<LucratividadeCategoria> resultado = new ArrayList<>();
        acumulado.forEach((categoria, v) -> {
            BigDecimal lucro = v[0].subtract(v[1]);
            BigDecimal margem = v[0].signum() == 0 ? BigDecimal.ZERO
                    : lucro.multiply(BigDecimal.valueOf(100)).divide(v[0], 2, RoundingMode.HALF_UP);
            resultado.add(new LucratividadeCategoria(categoria, quantidade.get(categoria), v[0], v[1], lucro, margem));
        });
        resultado.sort(Comparator.comparing(LucratividadeCategoria::margemPercentual).reversed());
        return resultado;
    }
}
