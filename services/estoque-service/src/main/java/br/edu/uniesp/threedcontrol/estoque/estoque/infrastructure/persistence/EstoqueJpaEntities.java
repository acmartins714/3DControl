package br.edu.uniesp.threedcontrol.estoque.estoque.infrastructure.persistence;

import br.edu.uniesp.threedcontrol.estoque.estoque.domain.ItemEstoque;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.MovimentacaoEstoque;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.Reserva;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

final class EstoqueJpaEntities {

    private EstoqueJpaEntities() {
    }

    @Entity
    @Table(name = "item_estoque")
    static class ItemEstoqueJpa {
        @Id
        UUID id;
        @Column(name = "insumo_id")
        UUID insumoId;
        @Column(name = "nome_insumo")
        String nomeInsumo;
        @Column(name = "unidade_medida")
        String unidadeMedida;
        @Column(name = "quantidade_disponivel")
        BigDecimal quantidadeDisponivel;
        @Column(name = "quantidade_reservada")
        BigDecimal quantidadeReservada;
        @Column(name = "estoque_minimo")
        BigDecimal estoqueMinimo;
        @Version
        Long versao;

        protected ItemEstoqueJpa() {
        }

        static ItemEstoqueJpa from(ItemEstoque i) {
            ItemEstoqueJpa e = new ItemEstoqueJpa();
            e.id = i.getId();
            e.insumoId = i.getInsumoId();
            e.nomeInsumo = i.getNomeInsumo();
            e.unidadeMedida = i.getUnidadeMedida();
            e.quantidadeDisponivel = i.getQuantidadeDisponivel();
            e.quantidadeReservada = i.getQuantidadeReservada();
            e.estoqueMinimo = i.getEstoqueMinimo();
            e.versao = i.getVersao();
            return e;
        }

        ItemEstoque toDomain() {
            return ItemEstoque.restaurar(id, insumoId, nomeInsumo, unidadeMedida, quantidadeDisponivel,
                    quantidadeReservada, estoqueMinimo, versao);
        }
    }

    @Entity
    @Table(name = "reserva")
    static class ReservaJpa {
        @Id
        UUID id;
        @Column(name = "pedido_id")
        UUID pedidoId;
        @Column(name = "insumo_id")
        UUID insumoId;
        BigDecimal quantidade;
        @Enumerated(EnumType.STRING)
        Reserva.Status status;
        @Column(name = "criada_em")
        LocalDateTime criadaEm;

        protected ReservaJpa() {
        }

        static ReservaJpa from(Reserva r) {
            ReservaJpa e = new ReservaJpa();
            e.id = r.getId();
            e.pedidoId = r.getPedidoId();
            e.insumoId = r.getInsumoId();
            e.quantidade = r.getQuantidade();
            e.status = r.getStatus();
            e.criadaEm = r.getCriadaEm();
            return e;
        }

        Reserva toDomain() {
            return Reserva.restaurar(id, pedidoId, insumoId, quantidade, status, criadaEm);
        }
    }

    @Entity
    @Table(name = "movimentacao_estoque")
    static class MovimentacaoJpa {
        @Id
        UUID id;
        @Column(name = "insumo_id")
        UUID insumoId;
        @Enumerated(EnumType.STRING)
        MovimentacaoEstoque.Tipo tipo;
        BigDecimal quantidade;
        @Column(name = "pedido_id")
        UUID pedidoId;
        String observacao;
        @Column(name = "data_hora")
        LocalDateTime dataHora;

        protected MovimentacaoJpa() {
        }

        static MovimentacaoJpa from(MovimentacaoEstoque m) {
            MovimentacaoJpa e = new MovimentacaoJpa();
            e.id = m.id();
            e.insumoId = m.insumoId();
            e.tipo = m.tipo();
            e.quantidade = m.quantidade();
            e.pedidoId = m.pedidoId();
            e.observacao = m.observacao();
            e.dataHora = m.dataHora();
            return e;
        }

        MovimentacaoEstoque toDomain() {
            return new MovimentacaoEstoque(id, insumoId, tipo, quantidade, pedidoId, observacao, dataHora);
        }
    }
}
