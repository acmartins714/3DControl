package br.edu.uniesp.threedcontrol.producao.ordemproducao;

import br.edu.uniesp.threedcontrol.producao.ordemproducao.OrdemProducaoDtos.OrdemResponse;
import br.edu.uniesp.threedcontrol.producao.ordemproducao.OrdemProducaoDtos.PlanejamentoRequest;
import br.edu.uniesp.threedcontrol.producao.shared.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Planejamento da fila (acionado pelo pedido-service) e ações do chão de fábrica.
 * Ao iniciar a produção, o pedido-service é notificado e passa o pedido para EM_PRODUCAO:
 * a partir desse momento o pedido fica BLOQUEADO para alterações (Regra de Ouro).
 */
@Service
@Transactional
public class OrdemProducaoService {

    private final OrdemProducaoRepository repository;
    private final PedidoClient pedidos;

    public OrdemProducaoService(OrdemProducaoRepository repository, PedidoClient pedidos) {
        this.repository = repository;
        this.pedidos = pedidos;
    }

    /** Cria a ordem do pedido ou, se já existir uma ativa, replaneja prazo e disponibilidade de insumos. */
    public OrdemResponse planejar(PlanejamentoRequest req) {
        OrdemProducao ordem = repository.findFirstByPedidoIdOrderByCriadaEmDesc(req.pedidoId())
                .filter(o -> o.getStatus() != StatusOrdem.CANCELADA)
                .orElse(null);
        if (ordem == null) {
            ordem = repository.save(new OrdemProducao(req.pedidoId(), req.codigoPedido(), req.prazoEntrega(),
                    req.insumosDisponiveis()));
        } else {
            ordem.replanejar(req.prazoEntrega(), req.insumosDisponiveis());
        }
        return resposta(ordem);
    }

    public void cancelarPorPedido(UUID pedidoId) {
        repository.findFirstByPedidoIdOrderByCriadaEmDesc(pedidoId).ifPresent(OrdemProducao::cancelar);
    }

    public OrdemResponse iniciar(UUID ordemId, String responsavel) {
        OrdemProducao ordem = buscarEntidade(ordemId);
        ordem.iniciar(responsavel);
        pedidos.notificarInicioProducao(ordem.getPedidoId());
        return resposta(ordem);
    }

    public OrdemResponse registrarRetrabalho(UUID ordemId, String motivo) {
        OrdemProducao ordem = buscarEntidade(ordemId);
        ordem.registrarRetrabalho(motivo);
        return resposta(ordem);
    }

    public OrdemResponse concluir(UUID ordemId) {
        OrdemProducao ordem = buscarEntidade(ordemId);
        ordem.concluir();
        pedidos.notificarConclusaoProducao(ordem.getPedidoId());
        return resposta(ordem);
    }

    /** Fila de produção priorizada, consultada pela equipe fabril. */
    @Transactional(readOnly = true)
    public List<OrdemResponse> fila() {
        LocalDate hoje = LocalDate.now();
        List<OrdemProducao> abertas = repository.findByStatusIn(EnumSet.of(StatusOrdem.AGUARDANDO, StatusOrdem.EM_PRODUCAO));
        return ordenarFila(abertas, hoje).stream().map(o -> OrdemResponse.from(o, hoje)).toList();
    }

    @Transactional(readOnly = true)
    public OrdemResponse buscar(UUID id) {
        return resposta(buscarEntidade(id));
    }

    /** Ordem mais recente de um pedido (útil para o atendimento localizar a ordem a partir do pedido). */
    @Transactional(readOnly = true)
    public OrdemResponse buscarPorPedido(UUID pedidoId) {
        return resposta(repository.findFirstByPedidoIdOrderByCriadaEmDesc(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de produção do pedido", pedidoId)));
    }

    /**
     * Ordem da fila: 1) insumos disponíveis primeiro; 2) maior prioridade de entrega;
     * 3) prazo mais próximo; 4) ordem de chegada.
     */
    static List<OrdemProducao> ordenarFila(List<OrdemProducao> ordens, LocalDate hoje) {
        Comparator<OrdemProducao> comparador = Comparator
                .comparing(OrdemProducao::isInsumosDisponiveis).reversed()
                .thenComparing(o -> o.prioridade(hoje).peso(), Comparator.reverseOrder())
                .thenComparing(OrdemProducao::getPrazoEntrega)
                .thenComparing(OrdemProducao::getCriadaEm);
        return ordens.stream().sorted(comparador).toList();
    }

    private OrdemProducao buscarEntidade(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de produção", id));
    }

    private static OrdemResponse resposta(OrdemProducao ordem) {
        return OrdemResponse.from(ordem, LocalDate.now());
    }
}
