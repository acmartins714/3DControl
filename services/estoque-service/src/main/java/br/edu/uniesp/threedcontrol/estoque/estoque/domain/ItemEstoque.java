package br.edu.uniesp.threedcontrol.estoque.estoque.domain;

import br.edu.uniesp.threedcontrol.estoque.shared.domain.DomainException;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Saldo de um insumo no estoque. Separa a quantidade física ({@code quantidadeDisponivel})
 * da quantidade já comprometida com pedidos ({@code quantidadeReservada}).
 */
public class ItemEstoque {

    private final UUID id;
    private final UUID insumoId;
    private String nomeInsumo;
    private String unidadeMedida;
    private BigDecimal quantidadeDisponivel;
    private BigDecimal quantidadeReservada;
    private BigDecimal estoqueMinimo;
    private final Long versao;

    private ItemEstoque(UUID id, UUID insumoId, String nomeInsumo, String unidadeMedida,
                        BigDecimal quantidadeDisponivel, BigDecimal quantidadeReservada, BigDecimal estoqueMinimo,
                        Long versao) {
        this.versao = versao;
        this.id = id;
        this.insumoId = insumoId;
        this.nomeInsumo = nomeInsumo;
        this.unidadeMedida = unidadeMedida;
        this.quantidadeDisponivel = quantidadeDisponivel;
        this.quantidadeReservada = quantidadeReservada;
        this.estoqueMinimo = estoqueMinimo;
    }

    public static ItemEstoque novo(UUID insumoId, String nomeInsumo, String unidadeMedida, BigDecimal estoqueMinimo) {
        if (insumoId == null) {
            throw new DomainException("O insumo é obrigatório.");
        }
        if (estoqueMinimo == null || estoqueMinimo.signum() < 0) {
            throw new DomainException("O estoque mínimo não pode ser negativo.");
        }
        return new ItemEstoque(UUID.randomUUID(), insumoId, nomeInsumo, unidadeMedida,
                BigDecimal.ZERO, BigDecimal.ZERO, estoqueMinimo, null);
    }

    public static ItemEstoque restaurar(UUID id, UUID insumoId, String nomeInsumo, String unidadeMedida,
                                        BigDecimal disponivel, BigDecimal reservada, BigDecimal minimo, Long versao) {
        return new ItemEstoque(id, insumoId, nomeInsumo, unidadeMedida, disponivel, reservada, minimo, versao);
    }

    public void registrarEntrada(BigDecimal quantidade) {
        exigirPositiva(quantidade);
        quantidadeDisponivel = quantidadeDisponivel.add(quantidade);
    }

    /** Saldo que ainda pode ser comprometido com novos pedidos. */
    public BigDecimal saldoLivre() {
        return quantidadeDisponivel.subtract(quantidadeReservada);
    }

    public boolean possuiSaldoPara(BigDecimal quantidade) {
        return saldoLivre().compareTo(quantidade) >= 0;
    }

    public void reservar(BigDecimal quantidade) {
        exigirPositiva(quantidade);
        if (!possuiSaldoPara(quantidade)) {
            throw new DomainException("Saldo insuficiente do insumo '" + nomeInsumo + "' para reserva.");
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
            throw new DomainException("Não há reserva suficiente do insumo '" + nomeInsumo + "' para consumo.");
        }
        quantidadeReservada = quantidadeReservada.subtract(quantidade);
        quantidadeDisponivel = quantidadeDisponivel.subtract(quantidade);
    }

    public boolean abaixoDoMinimo() {
        return saldoLivre().compareTo(estoqueMinimo) < 0;
    }

    public void atualizarParametros(String nomeInsumo, String unidadeMedida, BigDecimal estoqueMinimo) {
        if (estoqueMinimo == null || estoqueMinimo.signum() < 0) {
            throw new DomainException("O estoque mínimo não pode ser negativo.");
        }
        this.nomeInsumo = nomeInsumo;
        this.unidadeMedida = unidadeMedida;
        this.estoqueMinimo = estoqueMinimo;
    }

    private static void exigirPositiva(BigDecimal q) {
        if (q == null || q.signum() <= 0) {
            throw new DomainException("A quantidade deve ser maior que zero.");
        }
    }

    public UUID getId() { return id; }
    public UUID getInsumoId() { return insumoId; }
    public String getNomeInsumo() { return nomeInsumo; }
    public String getUnidadeMedida() { return unidadeMedida; }
    public BigDecimal getQuantidadeDisponivel() { return quantidadeDisponivel; }
    public BigDecimal getQuantidadeReservada() { return quantidadeReservada; }
    public BigDecimal getEstoqueMinimo() { return estoqueMinimo; }
    /** Versão do agregado: evita que duas reservas simultâneas consumam o mesmo saldo. */
    public Long getVersao() { return versao; }
}
