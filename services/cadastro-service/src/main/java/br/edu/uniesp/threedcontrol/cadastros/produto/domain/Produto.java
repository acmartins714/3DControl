package br.edu.uniesp.threedcontrol.cadastros.produto.domain;

import br.edu.uniesp.threedcontrol.cadastros.shared.domain.DomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** Item do catálogo de produtos personalizados, com sua composição de insumos (ficha técnica). */
public class Produto {

    private final UUID id;
    private String nome;
    private String descricao;
    private String categoria;
    private String especificacoesTecnicas;
    private int tempoMedioFabricacaoHoras;
    private BigDecimal precoBase;
    private List<ComposicaoProduto> composicao;
    private boolean ativo;

    private Produto(UUID id, String nome, String descricao, String categoria, String especificacoesTecnicas,
                    int tempoMedioFabricacaoHoras, BigDecimal precoBase, List<ComposicaoProduto> composicao,
                    boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.especificacoesTecnicas = especificacoesTecnicas;
        this.tempoMedioFabricacaoHoras = tempoMedioFabricacaoHoras;
        this.precoBase = precoBase;
        this.composicao = composicao;
        this.ativo = ativo;
    }

    public static Produto novo(String nome, String descricao, String categoria, String especificacoes,
                               int tempoMedioHoras, BigDecimal precoBase, List<ComposicaoProduto> composicao) {
        Produto p = new Produto(UUID.randomUUID(), nome, descricao, categoria, especificacoes,
                tempoMedioHoras, precoBase, List.of(), true);
        p.definirComposicao(composicao);
        p.validar();
        return p;
    }

    public static Produto restaurar(UUID id, String nome, String descricao, String categoria, String especificacoes,
                                    int tempoMedioHoras, BigDecimal precoBase, List<ComposicaoProduto> composicao,
                                    boolean ativo) {
        return new Produto(id, nome, descricao, categoria, especificacoes, tempoMedioHoras, precoBase,
                List.copyOf(composicao), ativo);
    }

    public void atualizar(String nome, String descricao, String categoria, String especificacoes,
                          int tempoMedioHoras, BigDecimal precoBase, List<ComposicaoProduto> composicao) {
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.especificacoesTecnicas = especificacoes;
        this.tempoMedioFabricacaoHoras = tempoMedioHoras;
        this.precoBase = precoBase;
        definirComposicao(composicao);
        validar();
    }

    private void definirComposicao(List<ComposicaoProduto> itens) {
        List<ComposicaoProduto> lista = itens == null ? List.of() : itens;
        Set<UUID> vistos = new HashSet<>();
        for (ComposicaoProduto c : lista) {
            if (!vistos.add(c.insumoId())) {
                throw new DomainException("O insumo " + c.insumoId() + " aparece mais de uma vez na composição.");
            }
        }
        this.composicao = List.copyOf(lista);
    }

    /** Custo dos insumos para fabricar uma unidade, a partir do custo unitário de cada insumo. */
    public BigDecimal calcularCustoInsumos(Map<UUID, BigDecimal> custoUnitarioPorInsumo) {
        return composicao.stream()
                .map(c -> c.quantidade().multiply(custoUnitarioPorInsumo.getOrDefault(c.insumoId(), BigDecimal.ZERO)))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Set<UUID> idsInsumos() {
        Set<UUID> ids = new LinkedHashSet<>();
        composicao.forEach(c -> ids.add(c.insumoId()));
        return ids;
    }

    public void inativar() {
        this.ativo = false;
    }

    private void validar() {
        if (nome == null || nome.isBlank()) {
            throw new DomainException("O nome do produto é obrigatório.");
        }
        if (categoria == null || categoria.isBlank()) {
            throw new DomainException("A categoria do produto é obrigatória.");
        }
        if (precoBase == null || precoBase.signum() < 0) {
            throw new DomainException("O preço base do produto não pode ser negativo.");
        }
        if (tempoMedioFabricacaoHoras < 0) {
            throw new DomainException("O tempo médio de fabricação não pode ser negativo.");
        }
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getCategoria() { return categoria; }
    public String getEspecificacoesTecnicas() { return especificacoesTecnicas; }
    public int getTempoMedioFabricacaoHoras() { return tempoMedioFabricacaoHoras; }
    public BigDecimal getPrecoBase() { return precoBase; }
    public List<ComposicaoProduto> getComposicao() { return composicao; }
    public boolean isAtivo() { return ativo; }
}
