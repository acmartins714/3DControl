package br.edu.uniesp.threedcontrol.estoque.estoque;

import br.edu.uniesp.threedcontrol.estoque.shared.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Saldo de um insumo no estoque. Separa a quantidade física ({@code quantidadeDisponivel})
 * da quantidade já comprometida com pedidos ({@code quantidadeReservada}).
 */
@Entity
@Table(name = "item_estoque")
public class ItemEstoque {

    @Id
    private UUID id;
    @Column(name = "insumo_id")
    private UUID insumoId;
    @Column(name = "nome_insumo")
    private String nomeInsumo;
    @Column(name = "unidade_medida")
    private String unidadeMedida;
    @Column(name = "quantidade_disponivel")
    private BigDecimal quantidadeDisponivel;
    @Column(name = "quantidade_reservada")
    private BigDecimal quantidadeReservada;
    @Column(name = "estoque_minimo")
    private BigDecimal estoqueMinimo;

    /** Controle de concorrência otimista: evita que duas reservas simultâneas consumam o mesmo saldo. */
    @Version
    private Long versao;

    protected ItemEstoque() {
    }

    public ItemEstoque(UUID insumoId, String nomeInsumo, String unidadeMedida, BigDecimal estoqueMinimo) {
        if (insumoId == null) {
            throw new RegraNegocioException("O insumo é obrigatório.");
        }
        this.id = UUID.randomUUID();
        this.insumoId = insumoId;
        this.quantidadeDisponivel = BigDecimal.ZERO;
        this.quantidadeReservada = BigDecimal.ZERO;
        atualizarParametros(nomeInsumo, unidadeMedida, estoqueMinimo);
    }

    public void atualizarParametros(String nomeInsumo, String unidadeMedida, BigDecimal estoqueMinimo) {
        if (estoqueMinimo == null || estoqueMinimo.signum() < 0) {
            throw new RegraNegocioException("O estoque mínimo não pode ser negativo.");
        }
        this.nomeInsumo = nomeInsumo;
        this.unidadeMedida = unidadeMedida;
        this.estoqueMinimo = estoqueMinimo;
    }

    public void registrarEntrada(BigDecimal quantidade) {
        exigirPositiva(quantidade);
        quantidadeDisponivel = quantidadeDisponivel.add(quantidade);
    }

    /** Saldo que ainda pode ser comprometido com novos pedidos. */
    public BigDecimal saldoLivre() {
        return quantidadeDisponivel.subtract(quantidadeReservada);
    }

    public void reservar(BigDecimal quantidade) {
        exigirPositiva(quantidade);
        if (saldoLivre().compareTo(quantidade) < 0) {
            throw new RegraNegocioException("Saldo insuficiente do insumo '" + nomeInsumo + "' para reserva.");
        }
        quantidadeReservada = quantidadeReservada.add(quantidade);
    }

    public void liberarReserva(BigDecimal quantidade) {
        exigirPositiva(quantidade);
        quantidadeReservada = quantidadeReservada.subtract(quantidade).max(BigDecimal.ZERO);
    }

    /** Baixa definitiva: o material reservado foi usado na produção. */
    public void consumirReserva(BigDecimal quantidade) {
        exigirPositiva(quantidade);
        if (quantidadeReservada.compareTo(quantidade) < 0) {
            throw new RegraNegocioException("Não há reserva suficiente do insumo '" + nomeInsumo + "' para consumo.");
        }
        quantidadeReservada = quantidadeReservada.subtract(quantidade);
        quantidadeDisponivel = quantidadeDisponivel.subtract(quantidade);
    }

    public boolean abaixoDoMinimo() {
        return saldoLivre().compareTo(estoqueMinimo) < 0;
    }

    private static void exigirPositiva(BigDecimal q) {
        if (q == null || q.signum() <= 0) {
            throw new RegraNegocioException("A quantidade deve ser maior que zero.");
        }
    }

    public UUID getId() { return id; }
    public UUID getInsumoId() { return insumoId; }
    public String getNomeInsumo() { return nomeInsumo; }
    public String getUnidadeMedida() { return unidadeMedida; }
    public BigDecimal getQuantidadeDisponivel() { return quantidadeDisponivel; }
    public BigDecimal getQuantidadeReservada() { return quantidadeReservada; }
    public BigDecimal getEstoqueMinimo() { return estoqueMinimo; }
}
