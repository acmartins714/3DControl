package br.edu.uniesp.threedcontrol.cadastros.insumo.infrastructure.config;

import br.edu.uniesp.threedcontrol.cadastros.insumo.application.GerenciarInsumosUseCase;
import br.edu.uniesp.threedcontrol.cadastros.insumo.domain.InsumoRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class InsumoConfig {

    @Bean
    GerenciarInsumosUseCase gerenciarInsumosUseCase(InsumoRepository repository) {
        return new GerenciarInsumosUseCase(repository);
    }
}
