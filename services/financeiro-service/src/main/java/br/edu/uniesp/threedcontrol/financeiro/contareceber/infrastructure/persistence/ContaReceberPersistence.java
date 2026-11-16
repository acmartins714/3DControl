package br.edu.uniesp.threedcontrol.financeiro.contareceber.infrastructure.persistence;

import br.edu.uniesp.threedcontrol.financeiro.contareceber.domain.*;
import jakarta.persistence.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Embeddable
class ItemContaEmbeddable {
    String categoria;
    @Column(name = "valor_venda")
    BigDecimal valorVenda;
    @Column(name = "custo_estimado")
    BigDecimal custoEstimado;

    protected ItemContaEmbeddable() {
    }

    ItemContaEmbeddable(ItemConta i) {
        this.categoria = i.categoria();
        this.valorVenda = i.valorVenda();
        this.custoEstimado = i.custoEstimado();
    }
}

@Embeddable
class PagamentoEmbeddable {
    @Column(name = "pagamento_id")
    UUID id;
    BigDecimal valor;
    @Enumerated(EnumType.STRING)
    Pagamento.TipoPagamento tipo;
    @Enumerated(EnumType.STRING)
    Pagamento.FormaPagamento forma;
    @Column(name = "pago_em")
    LocalDateTime pagoEm;
    String observacao;

    protected PagamentoEmbeddable() {
    }

    PagamentoEmbeddable(Pagamento p) {
        this.id = p.id();
        this.valor = p.valor();
        this.tipo = p.tipo();
        this.forma = p.forma();
        this.pagoEm = p.pagoEm();
        this.observacao = p.observacao();
    }
}

@Entity
@Table(name = "conta_receber")
class ContaReceberJpa {
    @Id
    UUID id;
    @Column(name = "pedido_id")
    UUID pedidoId;
    @Column(name = "cliente_id")
    UUID clienteId;
    @Enumerated(EnumType.STRING)
    StatusConta status;
    @Column(name = "criada_em")
    LocalDateTime criadaEm;
    @Column(name = "atualizada_em")
    LocalDateTime atualizadaEm;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "conta_receber_item", joinColumns = @JoinColumn(name = "conta_id"))
    List<ItemContaEmbeddable> itens = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pagamento", joinColumns = @JoinColumn(name = "conta_id"))
    @OrderBy("pagoEm")
    List<PagamentoEmbeddable> pagamentos = new ArrayList<>();

    protected ContaReceberJpa() {
    }

    static ContaReceberJpa from(ContaReceber c) {
        ContaReceberJpa e = new ContaReceberJpa();
        e.id = c.getId();
        e.pedidoId = c.getPedidoId();
        e.clienteId = c.getClienteId();
        e.status = c.getStatus();
        e.criadaEm = c.getCriadaEm();
        e.atualizadaEm = c.getAtualizadaEm();
        c.getItens().forEach(i -> e.itens.add(new ItemContaEmbeddable(i)));
        c.getPagamentos().forEach(p -> e.pagamentos.add(new PagamentoEmbeddable(p)));
        return e;
    }

    ContaReceber toDomain() {
        List<ItemConta> is = itens.stream().map(i -> new ItemConta(i.categoria, i.valorVenda, i.custoEstimado)).toList();
        List<Pagamento> ps = pagamentos.stream()
                .map(p -> new Pagamento(p.id, p.valor, p.tipo, p.forma, p.pagoEm, p.observacao)).toList();
        return ContaReceber.restaurar(id, pedidoId, clienteId, is, ps, status, criadaEm, atualizadaEm);
    }
}

interface ContaReceberJpaRepository extends JpaRepository<ContaReceberJpa, UUID> {
    Optional<ContaReceberJpa> findByPedidoId(UUID pedidoId);
    List<ContaReceberJpa> findByStatusOrderByCriadaEmDesc(StatusConta status);
    List<ContaReceberJpa> findByCriadaEmBetween(LocalDateTime inicio, LocalDateTime fim);
}

@Component
class ContaReceberRepositoryAdapter implements ContaReceberRepository {

    private final ContaReceberJpaRepository jpa;

    ContaReceberRepositoryAdapter(ContaReceberJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ContaReceber salvar(ContaReceber conta) {
        return jpa.save(ContaReceberJpa.from(conta)).toDomain();
    }

    @Override
    public Optional<ContaReceber> buscarPorPedidoId(UUID pedidoId) {
        return jpa.findByPedidoId(pedidoId).map(ContaReceberJpa::toDomain);
    }

    @Override
    public List<ContaReceber> listar(StatusConta status) {
        var lista = status == null ? jpa.findAll(Sort.by(Sort.Direction.DESC, "criadaEm"))
                : jpa.findByStatusOrderByCriadaEmDesc(status);
        return lista.stream().map(ContaReceberJpa::toDomain).toList();
    }

    @Override
    public List<ContaReceber> listarCriadasEntre(LocalDateTime inicio, LocalDateTime fim) {
        return jpa.findByCriadaEmBetween(inicio, fim).stream().map(ContaReceberJpa::toDomain).toList();
    }
}
