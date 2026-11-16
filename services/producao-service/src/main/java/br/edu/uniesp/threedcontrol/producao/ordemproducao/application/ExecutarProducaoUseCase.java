package br.edu.uniesp.threedcontrol.producao.ordemproducao.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.edu.uniesp.threedcontrol.producao.ordemproducao.application.port.PedidoGateway;
import br.edu.uniesp.threedcontrol.producao.ordemproducao.domain.OrdemProducao;
import br.edu.uniesp.threedcontrol.producao.ordemproducao.domain.OrdemProducaoRepository;
import br.edu.uniesp.threedcontrol.producao.shared.domain.RecursoNaoEncontradoException;

import java.util.UUID;

/**
 * Ações do chão de fábrica. Ao iniciar a produção, o pedido-service é notificado e passa o pedido
 * para EM_PRODUCAO — a partir desse momento o pedido fica BLOQUEADO para alterações (Regra de Ouro).
 */
@Service
@Transactional
public class ExecutarProducaoUseCase {

    private final OrdemProducaoRepository repository;
    private final PedidoGateway pedidos;

    public ExecutarProducaoUseCase(OrdemProducaoRepository repository, PedidoGateway pedidos) {
        this.repository = repository;
        this.pedidos = pedidos;
    }

    public OrdemProducao iniciar(UUID ordemId, String responsavel) {
        OrdemProducao ordem = buscar(ordemId);
        ordem.iniciar(responsavel);
        pedidos.notificarInicioProducao(ordem.getPedidoId());
        return repository.salvar(ordem);
    }

    public OrdemProducao registrarRetrabalho(UUID ordemId, String motivo) {
        OrdemProducao ordem = buscar(ordemId);
        ordem.registrarRetrabalho(motivo);
        return repository.salvar(ordem);
    }

    public OrdemProducao concluir(UUID ordemId) {
        OrdemProducao ordem = buscar(ordemId);
        ordem.concluir();
        pedidos.notificarConclusaoProducao(ordem.getPedidoId());
        return repository.salvar(ordem);
    }

    private OrdemProducao buscar(UUID id) {
        return repository.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de produção", id));
    }
}
