package br.edu.uniesp.threedcontrol.financeiro.lucratividade.domain;

import br.edu.uniesp.threedcontrol.financeiro.contareceber.domain.ContaReceber;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.domain.ItemConta;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.domain.StatusConta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** Serviço de domínio: consolida receita, custo e margem por categoria de produto. */
public final class AnaliseLucratividade {

    public record LucratividadeCategoria(String categoria, long quantidadeItens, BigDecimal receita,
                                         BigDecimal custo, BigDecimal lucro, BigDecimal margemPercentual) {
    }

    private AnaliseLucratividade() {
    }

    public static List<LucratividadeCategoria> porCategoria(Collection<ContaReceber> contas) {
        Map<String, BigDecimal[]> acumulado = new TreeMap<>();
        Map<String, Long> quantidade = new HashMap<>();
        for (ContaReceber conta : contas) {
            if (conta.getStatus() == StatusConta.CANCELADA) {
                continue;
            }
            for (ItemConta item : conta.getItens()) {
                BigDecimal[] valores = acumulado.computeIfAbsent(item.categoria(),
                        k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                valores[0] = valores[0].add(item.valorVenda());
                valores[1] = valores[1].add(item.custoEstimado());
                quantidade.merge(item.categoria(), 1L, Long::sum);
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
