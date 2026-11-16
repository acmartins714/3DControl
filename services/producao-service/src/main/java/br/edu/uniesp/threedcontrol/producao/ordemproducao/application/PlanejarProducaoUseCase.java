package br.edu.uniesp.threedcontrol.producao.ordemproducao.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.edu.uniesp.threedcontrol.producao.ordemproducao.domain.*;
import br.edu.uniesp.threedcontrol.producao.shared.domain.RecursoNaoEncontradoException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/** Inclusão/atualização de ordens na fila (acionado pelo pedido-service) e consulta da fila priorizada. */
@Service
@Transactional
public class PlanejarProducaoUseCase {

    public record ItemFila(OrdemProducao ordem, Prioridade prioridade, boolean atrasada) {
    }

    private final OrdemProducaoRepository repository;
    private final Clock clock;

    public PlanejarProducaoUseCase(OrdemProducaoRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Cria a ordem do pedido ou, se já existir, replaneja prazo e disponibilidade de insumos. */
    public OrdemProducao planejar(UUID pedidoId, String codigoPedido, LocalDate prazoEntrega, boolean insumosDisponiveis) {
        OrdemProducao ordem = repository.buscarPorPedidoId(pedidoId)
                .filter(o -> o.getStatus() != StatusOrdem.CANCELADA)
                .map(o -> {
                    o.replanejar(prazoEntrega, insumosDisponiveis);
                    return o;
                })
                .orElseGet(() -> OrdemProducao.nova(pedidoId, codigoPedido, prazoEntrega, insumosDisponiveis));
        return repository.salvar(ordem);
    }

    public void cancelarPorPedido(UUID pedidoId) {
        repository.buscarPorPedidoId(pedidoId).ifPresent(o -> {
            o.cancelar();
            repository.salvar(o);
        });
    }

    @Transactional(readOnly = true)
    public List<ItemFila> fila() {
        LocalDate hoje = LocalDate.now(clock);
        List<OrdemProducao> abertas = repository.listarPorStatus(EnumSet.of(StatusOrdem.AGUARDANDO, StatusOrdem.EM_PRODUCAO));
        return FilaProducao.ordenar(abertas, hoje).stream()
                .map(o -> new ItemFila(o, o.prioridade(hoje), o.estaAtrasada(hoje)))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrdemProducao buscar(UUID id) {
        return repository.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de produção", id));
    }

    /** Ordem mais recente de um pedido (útil para o atendimento localizar a ordem a partir do pedido). */
    @Transactional(readOnly = true)
    public OrdemProducao buscarPorPedido(UUID pedidoId) {
        return repository.buscarPorPedidoId(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de produção do pedido", pedidoId));
    }
}
