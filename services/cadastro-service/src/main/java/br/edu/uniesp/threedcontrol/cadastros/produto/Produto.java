package br.edu.uniesp.threedcontrol.cadastros.produto;

import br.edu.uniesp.threedcontrol.cadastros.shared.RegraNegocioException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Item do catálogo de produtos personalizados, com sua composição de insumos (ficha técnica). */
@Entity
@Table(name = "produto")
public class Produto {

    @Id
    private UUID id;
    private String nome;
    private String descricao;
    private String categoria;
    @Column(name = "especificacoes_tecnicas")
    private String especificacoesTecnicas;
    @Column(name = "tempo_medio_fabricacao_horas")
    private int tempoMedioFabricacaoHoras;
    @Column(name = "preco_base")
    private BigDecimal precoBase;
    private boolean ativo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "produto_composicao", joinColumns = @JoinColumn(name = "produto_id"))
    private List<ItemComposicao> composicao = new ArrayList<>();

    protected Produto() {
    }

    public Produto(String nome, String descricao, String categoria, String especificacoesTecnicas,
                   int tempoMedioFabricacaoHoras, BigDecimal precoBase, List<ItemComposicao> composicao) {
        this.id = UUID.randomUUID();
        this.ativo = true;
        atualizar(nome, descricao, categoria, especificacoesTecnicas, tempoMedioFabricacaoHoras, precoBase, composicao);
    }

    public void atualizar(String nome, String descricao, String categoria, String especificacoesTecnicas,
                          int tempoMedioFabricacaoHoras, BigDecimal precoBase, List<ItemComposicao> composicao) {
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.especificacoesTecnicas = especificacoesTecnicas;
        this.tempoMedioFabricacaoHoras = tempoMedioFabricacaoHoras;
        this.precoBase = precoBase;
        definirComposicao(composicao);
        validar();
    }

    private void definirComposicao(List<ItemComposicao> itens) {
        List<ItemComposicao> lista = itens == null ? List.of() : itens;
        Set<UUID> vistos = new HashSet<>();
        for (ItemComposicao c : lista) {
            if (!vistos.add(c.getInsumoId())) {
                throw new RegraNegocioException("O insumo " + c.getInsumoId() + " aparece mais de uma vez na composição.");
            }
        }
        this.composicao.clear();
        this.composicao.addAll(lista);
    }

    /** Custo dos insumos para fabricar uma unidade, a partir do custo unitário de cada insumo. */
    public BigDecimal calcularCustoInsumos(Map<UUID, BigDecimal> custoUnitarioPorInsumo) {
        return composicao.stream()
                .map(c -> c.getQuantidade().multiply(custoUnitarioPorInsumo.getOrDefault(c.getInsumoId(), BigDecimal.ZERO)))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Set<UUID> idsInsumos() {
        Set<UUID> ids = new LinkedHashSet<>();
        composicao.forEach(c -> ids.add(c.getInsumoId()));
        return ids;
    }

    public void inativar() {
        this.ativo = false;
    }

    private void validar() {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("O nome do produto é obrigatório.");
        }
        if (categoria == null || categoria.isBlank()) {
            throw new RegraNegocioException("A categoria do produto é obrigatória.");
        }
        if (precoBase == null || precoBase.signum() < 0) {
            throw new RegraNegocioException("O preço base do produto não pode ser negativo.");
        }
        if (tempoMedioFabricacaoHoras < 0) {
            throw new RegraNegocioException("O tempo médio de fabricação não pode ser negativo.");
        }
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getCategoria() { return categoria; }
    public String getEspecificacoesTecnicas() { return especificacoesTecnicas; }
    public int getTempoMedioFabricacaoHoras() { return tempoMedioFabricacaoHoras; }
    public BigDecimal getPrecoBase() { return precoBase; }
    public List<ItemComposicao> getComposicao() { return Collections.unmodifiableList(composicao); }
    public boolean isAtivo() { return ativo; }
}
