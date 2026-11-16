package br.edu.uniesp.threedcontrol.producao.ordemproducao.infrastructure.config;

import br.edu.uniesp.threedcontrol.producao.ordemproducao.application.ExecutarProducaoUseCase;
import br.edu.uniesp.threedcontrol.producao.ordemproducao.application.PlanejarProducaoUseCase;
import br.edu.uniesp.threedcontrol.producao.ordemproducao.application.port.PedidoGateway;
import br.edu.uniesp.threedcontrol.producao.ordemproducao.domain.OrdemProducaoRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class OrdemProducaoConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    PlanejarProducaoUseCase planejarProducaoUseCase(OrdemProducaoRepository repository, Clock clock) {
        return new PlanejarProducaoUseCase(repository, clock);
    }

    @Bean
    ExecutarProducaoUseCase executarProducaoUseCase(OrdemProducaoRepository repository, PedidoGateway pedidos) {
        return new ExecutarProducaoUseCase(repository, pedidos);
    }
}
