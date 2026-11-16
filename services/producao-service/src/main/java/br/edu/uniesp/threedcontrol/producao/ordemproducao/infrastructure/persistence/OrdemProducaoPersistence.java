package br.edu.uniesp.threedcontrol.producao.ordemproducao.infrastructure.persistence;

import br.edu.uniesp.threedcontrol.producao.ordemproducao.domain.OrdemProducao;
import br.edu.uniesp.threedcontrol.producao.ordemproducao.domain.OrdemProducaoRepository;
import br.edu.uniesp.threedcontrol.producao.ordemproducao.domain.StatusOrdem;
import jakarta.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "ordem_producao")
class OrdemProducaoJpa {
    @Id
    UUID id;
    @Column(name = "pedido_id")
    UUID pedidoId;
    @Column(name = "codigo_pedido")
    String codigoPedido;
    @Column(name = "prazo_entrega")
    LocalDate prazoEntrega;
    @Column(name = "insumos_disponiveis")
    boolean insumosDisponiveis;
    @Enumerated(EnumType.STRING)
    StatusOrdem status;
    String responsavel;
    int retrabalhos;
    String observacoes;
    @Column(name = "criada_em")
    LocalDateTime criadaEm;
    @Column(name = "iniciada_em")
    LocalDateTime iniciadaEm;
    @Column(name = "concluida_em")
    LocalDateTime concluidaEm;

    protected OrdemProducaoJpa() {
    }

    static OrdemProducaoJpa from(OrdemProducao o) {
        OrdemProducaoJpa e = new OrdemProducaoJpa();
        e.id = o.getId();
        e.pedidoId = o.getPedidoId();
        e.codigoPedido = o.getCodigoPedido();
        e.prazoEntrega = o.getPrazoEntrega();
        e.insumosDisponiveis = o.isInsumosDisponiveis();
        e.status = o.getStatus();
        e.responsavel = o.getResponsavel();
        e.retrabalhos = o.getRetrabalhos();
        e.observacoes = o.getObservacoes();
        e.criadaEm = o.getCriadaEm();
        e.iniciadaEm = o.getIniciadaEm();
        e.concluidaEm = o.getConcluidaEm();
        return e;
    }

    OrdemProducao toDomain() {
        return OrdemProducao.restaurar(id, pedidoId, codigoPedido, prazoEntrega, insumosDisponiveis, status,
                responsavel, retrabalhos, observacoes, criadaEm, iniciadaEm, concluidaEm);
    }
}

interface OrdemProducaoJpaRepository extends JpaRepository<OrdemProducaoJpa, UUID> {
    Optional<OrdemProducaoJpa> findFirstByPedidoIdOrderByCriadaEmDesc(UUID pedidoId);
    List<OrdemProducaoJpa> findByStatusIn(Collection<StatusOrdem> status);
}

@Component
class OrdemProducaoRepositoryAdapter implements OrdemProducaoRepository {

    private final OrdemProducaoJpaRepository jpa;

    OrdemProducaoRepositoryAdapter(OrdemProducaoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public OrdemProducao salvar(OrdemProducao ordem) {
        return jpa.save(OrdemProducaoJpa.from(ordem)).toDomain();
    }

    @Override
    public Optional<OrdemProducao> buscarPorId(UUID id) {
        return jpa.findById(id).map(OrdemProducaoJpa::toDomain);
    }

    @Override
    public Optional<OrdemProducao> buscarPorPedidoId(UUID pedidoId) {
        return jpa.findFirstByPedidoIdOrderByCriadaEmDesc(pedidoId).map(OrdemProducaoJpa::toDomain);
    }

    @Override
    public List<OrdemProducao> listarPorStatus(Collection<StatusOrdem> status) {
        return jpa.findByStatusIn(status).stream().map(OrdemProducaoJpa::toDomain).toList();
    }
}
