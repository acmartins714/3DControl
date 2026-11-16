package br.edu.uniesp.threedcontrol.pedidos.pedido.infrastructure.persistence;

import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.*;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.Enums.CanalAtendimento;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.Enums.CondicaoPagamento;
import br.edu.uniesp.threedcontrol.pedidos.pedido.domain.Enums.ModalidadeEntrega;
import jakarta.persistence.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Embeddable
class ItemPedidoEmbeddable {
    @Column(name = "produto_id")
    UUID produtoId;
    @Column(name = "nome_produto")
    String nomeProduto;
    String categoria;
    int quantidade;
    @Column(name = "preco_unitario")
    BigDecimal precoUnitario;
    @Column(name = "custo_unitario_estimado")
    BigDecimal custoUnitarioEstimado;
    String personalizacao;

    protected ItemPedidoEmbeddable() {
    }

    ItemPedidoEmbeddable(ItemPedido i) {
        produtoId = i.produtoId();
        nomeProduto = i.nomeProduto();
        categoria = i.categoria();
        quantidade = i.quantidade();
        precoUnitario = i.precoUnitario();
        custoUnitarioEstimado = i.custoUnitarioEstimado();
        personalizacao = i.personalizacao();
    }

    ItemPedido toDomain() {
        return new ItemPedido(produtoId, nomeProduto, categoria, quantidade, precoUnitario, custoUnitarioEstimado,
                personalizacao);
    }
}

@Embeddable
class HistoricoEmbeddable {
    @Enumerated(EnumType.STRING)
    StatusPedido status;
    @Column(name = "data_hora")
    LocalDateTime dataHora;
    String observacao;

    protected HistoricoEmbeddable() {
    }

    HistoricoEmbeddable(HistoricoStatus h) {
        status = h.status();
        dataHora = h.dataHora();
        observacao = h.observacao();
    }
}

@Entity
@Table(name = "pedido")
class PedidoJpa {
    @Id
    UUID id;
    String codigo;
    @Column(name = "cliente_id")
    UUID clienteId;
    @Enumerated(EnumType.STRING)
    @Column(name = "canal_origem")
    CanalAtendimento canalOrigem;
    @Column(name = "atendente_responsavel")
    String atendenteResponsavel;
    @Column(name = "prazo_entrega")
    LocalDate prazoEntrega;
    @Enumerated(EnumType.STRING)
    @Column(name = "modalidade_entrega")
    ModalidadeEntrega modalidadeEntrega;
    @Enumerated(EnumType.STRING)
    @Column(name = "condicao_pagamento")
    CondicaoPagamento condicaoPagamento;
    String observacoes;
    @Enumerated(EnumType.STRING)
    StatusPedido status;
    @Column(name = "criado_em")
    LocalDateTime criadoEm;
    @Column(name = "atualizado_em")
    LocalDateTime atualizadoEm;
    @Version
    Long versao;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pedido_item", joinColumns = @JoinColumn(name = "pedido_id"))
    List<ItemPedidoEmbeddable> itens = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pedido_historico", joinColumns = @JoinColumn(name = "pedido_id"))
    @OrderBy("dataHora")
    List<HistoricoEmbeddable> historico = new ArrayList<>();

    protected PedidoJpa() {
    }

    static PedidoJpa from(Pedido p) {
        PedidoJpa e = new PedidoJpa();
        e.id = p.getId();
        e.codigo = p.getCodigo();
        e.clienteId = p.getClienteId();
        e.canalOrigem = p.getCanalOrigem();
        e.atendenteResponsavel = p.getAtendenteResponsavel();
        e.prazoEntrega = p.getPrazoEntrega();
        e.modalidadeEntrega = p.getModalidadeEntrega();
        e.condicaoPagamento = p.getCondicaoPagamento();
        e.observacoes = p.getObservacoes();
        e.status = p.getStatus();
        e.criadoEm = p.getCriadoEm();
        e.atualizadoEm = p.getAtualizadoEm();
        e.versao = p.getVersao();
        p.getItens().forEach(i -> e.itens.add(new ItemPedidoEmbeddable(i)));
        p.getHistorico().forEach(h -> e.historico.add(new HistoricoEmbeddable(h)));
        return e;
    }

    Pedido toDomain() {
        return Pedido.restaurar(id, codigo, clienteId, canalOrigem, atendenteResponsavel, prazoEntrega,
                modalidadeEntrega, condicaoPagamento, observacoes,
                itens.stream().map(ItemPedidoEmbeddable::toDomain).toList(), status,
                historico.stream().map(h -> new HistoricoStatus(h.status, h.dataHora, h.observacao)).toList(),
                criadoEm, atualizadoEm, versao);
    }
}

interface PedidoJpaRepository extends JpaRepository<PedidoJpa, UUID> {
    Optional<PedidoJpa> findByCodigo(String codigo);
    List<PedidoJpa> findByStatusOrderByPrazoEntrega(StatusPedido status);
    List<PedidoJpa> findByClienteIdOrderByCriadoEmDesc(UUID clienteId);
    List<PedidoJpa> findByStatusAndClienteIdOrderByPrazoEntrega(StatusPedido status, UUID clienteId);
}

@Component
class PedidoRepositoryAdapter implements PedidoRepository {

    private final PedidoJpaRepository jpa;

    PedidoRepositoryAdapter(PedidoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Pedido salvar(Pedido pedido) {
        return jpa.save(PedidoJpa.from(pedido)).toDomain();
    }

    @Override
    public Optional<Pedido> buscarPorId(UUID id) {
        return jpa.findById(id).map(PedidoJpa::toDomain);
    }

    @Override
    public Optional<Pedido> buscarPorCodigo(String codigo) {
        return jpa.findByCodigo(codigo).map(PedidoJpa::toDomain);
    }

    @Override
    public List<Pedido> listar(StatusPedido status, UUID clienteId) {
        List<PedidoJpa> lista;
        if (status != null && clienteId != null) {
            lista = jpa.findByStatusAndClienteIdOrderByPrazoEntrega(status, clienteId);
        } else if (status != null) {
            lista = jpa.findByStatusOrderByPrazoEntrega(status);
        } else if (clienteId != null) {
            lista = jpa.findByClienteIdOrderByCriadoEmDesc(clienteId);
        } else {
            lista = jpa.findAll(Sort.by("prazoEntrega"));
        }
        return lista.stream().map(PedidoJpa::toDomain).toList();
    }
}
